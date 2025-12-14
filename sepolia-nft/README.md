# OpenWallet example NFT (Sepolia)

This folder contains a minimal **ERC-721** contract you can deploy to **Ethereum Sepolia** and mint NFTs to your wallet so they show up in OpenWallet’s **NFT Gallery**.

## Enhancements Delivered (Post-Review)

- **Multi-wallet support**

  - Multiple wallet profiles can be created/imported and selected at login.
  - Clear separation of wallet identity vs. on-chain actions.

- **ERC-20 token handling**

  - ERC-20 token support including contract-based balance reading and transfer flows.
  - Supports adding/importing token contracts and displaying token balances consistently.

- **Desktop notifications (transactions)**
  - Desktop notifications are shown for key events (e.g., successful send, failed send, confirmations where available).
  - Notifications are non-blocking and do not interrupt the user’s workflow.

## Prereqs

- Node.js 18+
- Some Sepolia ETH for gas (you said you have ~0.04 ETH, that’s plenty for a few deploy/mints)
- A Sepolia RPC URL (Alchemy/Infura/etc)

## Setup

PowerShell:

```powershell
cd c:\OpenWallet\sepolia-nft
npm install
copy .env.example .env
```

Edit `.env` and set:

- `SEPOLIA_RPC_URL`
- `DEPLOYER_PRIVATE_KEY` (with or without `0x` prefix)

Optional:

- `NFT_NAME`, `NFT_SYMBOL`
- `MINT_TO` (defaults to deployer)
- `TOKEN_URI`

## Compile

```powershell
npm run compile
```

## Deploy to Sepolia

```powershell
npm run deploy:sepolia
```

This writes the deployed contract address to `.deployed-address` (local file, gitignored).

## Mint an NFT

```powershell
npm run mint:sepolia
```

By default (when `TOKEN_URI` is empty), the mint script generates **inline metadata** and uses a deterministic **RoboHash PNG** image URL (unique per token), so OpenWallet will show distinct “cool” images without needing IPFS.

## See it in OpenWallet

1. Run OpenWallet
2. Login
3. Dashboard → **NFTs** → **Refresh**

If you minted to a different address, make sure the active wallet profile matches.

## Token URI tip

For testing, you can point `TOKEN_URI` to any URL that returns JSON metadata like:

```json
{
  "name": "OWTN #1",
  "description": "OpenWallet test NFT",
  "image": "https://placehold.co/512x512.png"
}
```

If your `image` uses `ipfs://...`, the gallery will try a default IPFS gateway.

If you want to override the default image/metadata entirely, set `TOKEN_URI` in `.env` to a URL or a `data:application/json;base64,...` token URI.
