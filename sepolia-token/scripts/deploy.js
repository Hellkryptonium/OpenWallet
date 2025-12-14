import * as dotenv from "dotenv";
import hre from "hardhat";

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

function parseUintEnv(name, fallback) {
  const raw = envOrDefault(name, String(fallback));
  const parsed = Number(raw);
  if (!Number.isFinite(parsed) || parsed < 0) throw new Error(`Invalid ${name}: ${raw}`);
  return parsed;
}

async function main() {
  // Ensure config is usable
  requireEnv("SEPOLIA_RPC_URL");
  requireEnv("DEPLOYER_PRIVATE_KEY");

  const [deployer] = await hre.ethers.getSigners();

  const tokenName = envOrDefault("TOKEN_NAME", "OpenWallet Test Token");
  const tokenSymbol = envOrDefault("TOKEN_SYMBOL", "OWTT");
  const tokenDecimals = parseUintEnv("TOKEN_DECIMALS", 18);
  const initialSupplyTokens = parseUintEnv("INITIAL_SUPPLY_TOKENS", 1_000_000);

  const initialSupply = hre.ethers.parseUnits(String(initialSupplyTokens), tokenDecimals);

  console.log("Deployer:", deployer.address);
  console.log("Deploying token:", { tokenName, tokenSymbol, tokenDecimals, initialSupplyTokens });

  const Token = await hre.ethers.getContractFactory("OpenWalletToken");
  const token = await Token.deploy(
    tokenName,
    tokenSymbol,
    tokenDecimals,
    deployer.address,
    initialSupply
  );

  await token.waitForDeployment();

  const address = await token.getAddress();
  console.log("Deployed OpenWalletToken to:", address);

  console.log("\nOpenWallet add-token fields:");
  console.log("- networkId: sepolia");
  console.log("- address:", address);
  console.log("- expected decimals:", tokenDecimals);
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
