import * as dotenv from "dotenv";
import hre from "hardhat";
import fs from "node:fs";

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

async function main() {
  requireEnv("SEPOLIA_RPC_URL");
  requireEnv("DEPLOYER_PRIVATE_KEY");

  const [deployer] = await hre.ethers.getSigners();

  const nftName = envOrDefault("NFT_NAME", "OpenWallet Test NFT");
  const nftSymbol = envOrDefault("NFT_SYMBOL", "OWTN");

  console.log("Deployer:", deployer.address);
  console.log("Deploying NFT collection:", { nftName, nftSymbol });

  const Factory = await hre.ethers.getContractFactory("OpenWalletNft");
  const nft = await Factory.deploy(nftName, nftSymbol, deployer.address);
  await nft.waitForDeployment();

  const address = await nft.getAddress();
  console.log("Deployed OpenWalletNft to:", address);

  // Store locally for convenience (ignored by .gitignore)
  fs.writeFileSync(".deployed-address", address, { encoding: "utf8" });

  console.log("\nNext:");
  console.log("- Set DEPLOYED_NFT_ADDRESS in .env OR keep .deployed-address file");
  console.log("- Run: npm run mint:sepolia");
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
