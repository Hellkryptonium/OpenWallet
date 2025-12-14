# OpenWallet example token (Sepolia)

This folder contains a minimal ERC-20 you can deploy to **Ethereum Sepolia** and then import into OpenWallet.

## Prereqs

- Node.js 18+ (recommended)
- Some Sepolia ETH for gas (faucet)
- A Sepolia RPC URL (Alchemy/Infura/etc)

## Setup

PowerShell:

```powershell
cd c:\OpenWallet\sepolia-token
npm install
copy .env.example .env
```

Edit `.env` and set:

- `SEPOLIA_RPC_URL`
- `DEPLOYER_PRIVATE_KEY` (no `0x` prefix)

Optionally customize token settings:

- `TOKEN_NAME`
- `TOKEN_SYMBOL`
- `TOKEN_DECIMALS`
- `INITIAL_SUPPLY_TOKENS`

## Compile

```powershell
npm run compile
```

## Deploy to Sepolia

```powershell
npm run deploy:sepolia
```

The script prints the deployed token address.

## Add the token in OpenWallet

OpenWallet already supports importing custom ERC-20 tokens.

1. Launch OpenWallet
2. Select the **Sepolia** network
3. Use **Add Token** and paste the deployed contract address
4. Confirm the token metadata (name/symbol/decimals)

OpenWallet will persist it to your user profile (typically `~/.openwallet/tokens.json`).

## Notes

- This token is **mintable by the owner** via `mint(to, amount)`.
- Amounts are in the smallest unit (like wei), so minting `1` with 18 decimals means `0.000000000000000001` tokens.
