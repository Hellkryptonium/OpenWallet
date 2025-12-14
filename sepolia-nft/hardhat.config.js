import "@nomicfoundation/hardhat-ethers";
import * as dotenv from "dotenv";

dotenv.config();

const { SEPOLIA_RPC_URL, DEPLOYER_PRIVATE_KEY } = process.env;

const normalizedPrivateKey = (() => {
  if (!DEPLOYER_PRIVATE_KEY) return "";
  const pk = DEPLOYER_PRIVATE_KEY.trim();
  if (!pk.length) return "";
  return pk.startsWith("0x") ? pk : `0x${pk}`;
})();

/** @type {import('hardhat/config').HardhatUserConfig} */
const config = {
  solidity: {
    version: "0.8.20",
    settings: {
      optimizer: { enabled: true, runs: 200 }
    }
  },
  networks: {
    sepolia: {
      url: SEPOLIA_RPC_URL || "",
      accounts: normalizedPrivateKey ? [normalizedPrivateKey] : []
    }
  }
};

export default config;
