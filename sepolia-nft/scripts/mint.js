import * as dotenv from "dotenv";
import hre from "hardhat";
import fs from "node:fs";
import crypto from "node:crypto";

dotenv.config();

function requireEnv(name) {
  const value = process.env[name];
  if (!value) throw new Error(`Missing env var: ${name}`);
  return value;
}

function envOrDefault(name, fallback) {
  const value = process.env[name];
  return value && value.length ? value : fallback;
}

function buildDataTokenUri(metadataObj) {
  const json = JSON.stringify(metadataObj);
  const b64 = Buffer.from(json, 'utf8').toString('base64');
  return `data:application/json;base64,${b64}`;
}

function sha256Hex(input) {
  return crypto.createHash('sha256').update(String(input), 'utf8').digest('hex');
}

function buildRoboHashImageUrl(seed) {
  // RoboHash returns PNGs and is easy for JavaFX to render.
  // Using a hash keeps the URL short-ish and stable.
  const h = sha256Hex(seed);
  return `https://robohash.org/${encodeURIComponent(h)}.png?size=512x512&set=set2&bgset=bg2`;
}

function resolveDeployedAddress() {
  const fromEnv = process.env.DEPLOYED_NFT_ADDRESS;
  if (fromEnv && fromEnv.trim().length) return fromEnv.trim();
  if (fs.existsSync(".deployed-address")) {
    const raw = fs.readFileSync(".deployed-address", "utf8");
    if (raw && raw.trim().length) return raw.trim();
  }
  return "";
}

async function main() {
  requireEnv("SEPOLIA_RPC_URL");
  requireEnv("DEPLOYER_PRIVATE_KEY");

  const deployed = resolveDeployedAddress();
  if (!deployed) {
    throw new Error("Missing deployed contract address. Set DEPLOYED_NFT_ADDRESS or run deploy first.");
  }

  const [deployer] = await hre.ethers.getSigners();
  const to = envOrDefault("MINT_TO", deployer.address);

  const configuredTokenUri = envOrDefault("TOKEN_URI", "").trim();
  const shouldUseInline =
    !configuredTokenUri ||
    configuredTokenUri.length === 0 ||
    configuredTokenUri === "https://placehold.co/metadata.json";

  console.log("Minting from:", deployer.address);
  console.log("Minting to:", to);
  console.log("Contract:", deployed);
  const nft = await hre.ethers.getContractAt("OpenWalletNft", deployed);

  // Predict tokenId for nicer metadata.
  let predictedId = null;
  try {
    predictedId = await nft.nextTokenId();
  } catch {
    // best-effort; contract supports it but keep script resilient
  }

  const tokenName = predictedId === null
    ? "OpenWallet Test NFT"
    : `OpenWallet Test NFT #${predictedId.toString()}`;

  const imageUrl = buildRoboHashImageUrl(`${deployed.toLowerCase()}:${String(predictedId ?? '')}:${to.toLowerCase()}`);
  const tokenUri = shouldUseInline
    ? buildDataTokenUri({
        name: tokenName,
        description: "OpenWallet testnet NFT (Sepolia)",
        image: imageUrl
      })
    : configuredTokenUri;

  console.log("predictedTokenId:", predictedId === null ? null : predictedId.toString());
  console.log("image:", shouldUseInline ? imageUrl : "(from TOKEN_URI)");
  console.log("tokenURI:", tokenUri);

  const tx = await nft.mintTo(to, tokenUri);
  console.log("Sent tx:", tx.hash);
  const receipt = await tx.wait();
  console.log("Mined in block:", receipt.blockNumber);

  // Best-effort tokenId extraction (standard Transfer event is emitted)
  const transferTopic = hre.ethers.id("Transfer(address,address,uint256)");
  const log = receipt.logs.find((l) => l.topics && l.topics[0] === transferTopic);
  if (log && log.topics && log.topics.length >= 4) {
    const tokenId = BigInt(log.topics[3]);
    console.log("Minted tokenId:", tokenId.toString());
  } else {
    console.log("Minted (tokenId not parsed). Refresh NFT gallery to see it.");
  }

  console.log("\nOpenWallet:");
  console.log("- Open Dashboard → NFTs → Refresh");
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
