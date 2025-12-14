import * as dotenv from "dotenv";
import hre from "hardhat";
import fs from "node:fs";

dotenv.config();

function requireEnv(name) {
  const value = process.env[name];
  if (!value) throw new Error(`Missing env var: ${name}`);
  return value;
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
  const owner = (process.argv[2] || deployer.address).trim();

  const nft = await hre.ethers.getContractAt("OpenWalletNft", deployed);

  const bal = await nft.balanceOf(owner);
  const next = await nft.nextTokenId();

  console.log("Contract:", deployed);
  console.log("Owner:", owner);
  console.log("balanceOf:", bal.toString());
  console.log("nextTokenId:", next.toString());

  const max = Number(next);
  const toCheck = Math.min(max, 10);
  for (let i = 0; i < toCheck; i++) {
    try {
      const o = await nft.ownerOf(i);
      console.log(`ownerOf(${i}):`, o);
    } catch (e) {
      console.log(`ownerOf(${i}): error`);
    }
  }
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
