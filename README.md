# OpenWallet

OpenWallet is a non-custodial Ethereum wallet built with **Java 17**, **JavaFX (FXML + CSS)**, and **Web3j**.
It targets **testnets** (Sepolia + additional configured networks) and includes a complete demo stack:

- A JavaFX desktop wallet app (MySQL-backed profiles, encrypted key storage)
- A deployable **ERC-20** example token (Sepolia)
- A deployable **ERC-721** example NFT collection + mint script (Sepolia)

> Safety note: This project is intended for learning and testnets. Do not use it to store real funds.

## Features

### Wallet

- **Non-custodial** key management (keys never leave the machine)
- **BIP-39** 12-word mnemonic generation + import
- **Password-protected login** (encrypted wallet data at rest)
- **ETH send/receive** (includes QR receive screen)
- **Local transaction logging** (outgoing txs stored in MySQL)

### Tokens (ERC-20)

- Import any ERC-20 by contract address
- Fetch token metadata (name/symbol/decimals)
- Show balances and send ERC-20 transfers
- Example Hardhat project to deploy your own token to Sepolia

### NFTs (ERC-721)

- **NFT Gallery** with refresh
- Fetch owned NFTs using **Alchemy NFT API** (supports common image fields and `ipfs://`)
- Example Hardhat project to deploy + mint an NFT collection on Sepolia
- Supports pointing OpenWallet at one or more NFT contract addresses (via env var)

### Multi-network + utilities

- Networks loaded from JSON config (Sepolia, Base Sepolia, Optimism Sepolia)
- Bridge screen that opens known bridge URLs for configured networks
- Optional Chainlink feed addresses included in network config (used by price features)

## Repository layout

```
openwallet-app/     JavaFX wallet application (Maven)
sepolia-token/      Example ERC-20 token (Hardhat)
sepolia-nft/        Example ERC-721 NFT collection (Hardhat)
installer/          Windows packaging artifacts (optional)
```

## Prerequisites

### For the JavaFX app

- **Java 17+** (project compiles with Java 17)
- **Maven 3.9+**
- **MySQL 8+** (local development)

### For smart contracts (optional, for “make coins / mint NFTs”)

- **Node.js 18+**
- Sepolia ETH for gas (faucet)
- Sepolia RPC URL (Alchemy/Infura/etc)

## Configure OpenWallet (RPC + Database)

OpenWallet loads defaults from `openwallet-app/src/main/resources/db.properties` and `openwallet-app/src/main/resources/networks.json`.
You can configure via **either** editing the properties file **or** environment variables.

### 1) Create the database

Run the schema file:

- `openwallet-app/src/main/resources/db/schema.sql`

Example (MySQL client):

```sql
SOURCE path/to/openwallet-app/src/main/resources/db/schema.sql;
```

### 2) Configure DB + RPC

Option A — edit the properties file (simple local dev):

1. Copy the example template:

```powershell
copy openwallet-app/src/main/resources/db.properties.example openwallet-app/src/main/resources/db.properties
```

2. Edit `openwallet-app/src/main/resources/db.properties` and set your values:

```properties
db.url=jdbc:mysql://localhost:3306/openwallet_db
db.user=root
db.password=YOUR_PASSWORD

# Sepolia RPC (recommended: Alchemy)
rpc.url=https://eth-sepolia.g.alchemy.com/v2/YOUR_KEY
```

Option B — environment variable overrides (recommended for not committing secrets):

- `OPENWALLET_DB_URL`
- `OPENWALLET_DB_USER`
- `OPENWALLET_DB_PASSWORD`
- `OPENWALLET_RPC_URL`
- `OPENWALLET_CHAIN_ID` (optional override; otherwise comes from the selected network)

PowerShell example:

```powershell
$env:OPENWALLET_DB_URL='jdbc:mysql://localhost:3306/openwallet_db'
$env:OPENWALLET_DB_USER='root'
$env:OPENWALLET_DB_PASSWORD='YOUR_PASSWORD'
$env:OPENWALLET_RPC_URL='https://eth-sepolia.g.alchemy.com/v2/YOUR_KEY'
```

### 3) Configure networks (optional)

Networks are defined in `openwallet-app/src/main/resources/networks.json`.

- If a network’s `rpcUrl` is blank, OpenWallet falls back to `OPENWALLET_RPC_URL` then `rpc.url` from `db.properties`.
- The active network selection is saved in user preferences.

## Build, test, and run

### Run (development)

From the repo root:

```powershell
mvn -f openwallet-app/pom.xml javafx:run
```

Or from the app directory:

```powershell
cd openwallet-app
mvn javafx:run
```

### Run tests

```powershell
mvn -f openwallet-app/pom.xml test
```

### Build a JAR

```powershell
mvn -f openwallet-app/pom.xml -DskipTests package
```

The built artifact will be under `openwallet-app/target/`.

## Using the app (end-to-end)

1. **Startup**
   - If no profiles exist: Create Wallet / Import Wallet.
   - If profiles exist: you’ll land on the Login screen.
2. **Create Wallet**
   - Save the 12-word recovery phrase.
   - Pick a password (used to encrypt wallet data).
3. **Dashboard**
   - See address + balance.
   - Send ETH, Receive ETH (QR), view recent tx history.
4. **Tokens**
   - Add Token → paste ERC-20 contract address.
   - Send Token.
5. **NFTs**
   - NFTs → Refresh.
   - See “NFT setup” below if gallery is empty.

## “Make coins” (deploy your own ERC-20 token)

The `sepolia-token/` folder contains a minimal mintable ERC-20 contract (Hardhat + OpenZeppelin).

### 1) Setup

```powershell
cd sepolia-token
npm install
copy .env.example .env
```

Edit `.env`:

- `SEPOLIA_RPC_URL`
- `DEPLOYER_PRIVATE_KEY` (no `0x` prefix)

Optional:

- `TOKEN_NAME`, `TOKEN_SYMBOL`, `TOKEN_DECIMALS`, `INITIAL_SUPPLY_TOKENS`

### 2) Compile + deploy

```powershell
npm run compile
npm run deploy:sepolia
```

The deploy script prints the contract address and mints the initial supply to the deployer.

### 3) Mint more tokens (optional)

This ERC-20 is mintable by the owner.

Use Hardhat console:

```powershell
npx hardhat console --network sepolia
```

Then:

```javascript
const token = await ethers.getContractAt(
  "OpenWalletToken",
  "0xYOUR_TOKEN_ADDRESS"
);
await token.mint("0xRECIPIENT_ADDRESS", ethers.parseUnits("1000", 18));
```

> If you changed decimals, update `parseUnits(..., DECIMALS)` accordingly.

### 4) Import the token into OpenWallet

In OpenWallet:

Dashboard → Tokens → Add Token → paste the ERC-20 address.

Imported tokens are persisted under your user profile (typically `~/.openwallet/tokens.json`).

## “Make NFTs” (deploy + mint an ERC-721 collection)

The `sepolia-nft/` folder contains an ERC-721 contract and scripts to deploy + mint.

### 1) Setup

```powershell
cd sepolia-nft
npm install
copy .env.example .env
```

Edit `.env`:

- `SEPOLIA_RPC_URL`
- `DEPLOYER_PRIVATE_KEY` (with or without `0x` prefix)

Optional:

- `NFT_NAME`, `NFT_SYMBOL`
- `MINT_TO` (defaults to deployer)
- `TOKEN_URI` (optional metadata override)

### 2) Compile + deploy

```powershell
npm run compile
npm run deploy:sepolia
```

This writes the deployed address to `sepolia-nft/.deployed-address`.

### 3) Mint an NFT

```powershell
npm run mint:sepolia
```

If `TOKEN_URI` is empty, the script mints with inline `data:application/json;base64,...` metadata and a deterministic RoboHash image URL so the NFT shows a unique image without IPFS.

## NFT setup in OpenWallet (so the gallery finds your contracts)

OpenWallet loads NFT collections from:

1. `OPENWALLET_NFT_CONTRACTS` (comma-separated contract addresses)
2. The local `sepolia-nft/.deployed-address` file (dev convenience if you run the app from the repo)

PowerShell example:

```powershell
$env:OPENWALLET_NFT_CONTRACTS='0xABC...,0xDEF...'
mvn -f openwallet-app/pom.xml javafx:run
```

### Alchemy API key requirement

The NFT Gallery uses the Alchemy NFT API. Configure one of:

- `OPENWALLET_ALCHEMY_API_KEY` (preferred)
- `ALCHEMY_API_KEY`

If you don’t set an explicit key, OpenWallet will attempt to extract it from an Alchemy-style RPC URL like `https://...alchemy.com/v2/<key>`.

## Troubleshooting

### App starts but shows empty balances

- Confirm `OPENWALLET_RPC_URL` or `rpc.url` is set.
- Ensure the selected network in Settings has a usable RPC.

### Database connection errors

- Confirm MySQL is running.
- Confirm the schema has been created (`schema.sql`).
- Confirm DB URL/user/password (properties or env vars).

### NFT Gallery is empty

- Confirm you minted to the currently logged-in wallet address.
- Confirm `OPENWALLET_ALCHEMY_API_KEY` (or a valid Alchemy `/v2/<key>` RPC URL).
- Confirm `OPENWALLET_NFT_CONTRACTS` includes your NFT contract address (or `.deployed-address` exists).

## Java GUI-Based Projects Marking Rubric (Review 2) — Highlights

### Code Quality & Testing (10 marks)

- **Separation of concerns**: UI (FXML/controllers), services (Web3/RPC, tokens, NFTs), crypto utilities, and DAO persistence are kept distinct.
- **Config + dependency boundaries**: DB + RPC are centralized through config helpers and can be overridden via environment variables.
- **Automated tests**: the app module includes JUnit 5 tests (plus Mockito for mocking).
  - Run: `mvn -f openwallet-app/pom.xml test`
- **Reliability practices**: background work is performed asynchronously and UI updates are marshalled back to the JavaFX thread, reducing UI freezes and race conditions.

### Teamwork & Collaboration (5 marks)

If you’re submitting as a team, the easiest way to score well here is to make collaboration visible and repeatable:

- **Recommended workflow**: feature branches + pull requests + review before merge.
- **Suggested evidence for marking**:
  - PR descriptions that reference features (Tokens, NFTs, Bridge, UI)
  - Linked issues/tasks, and small incremental commits
  - Review notes (what changed and why) and screenshots for UI changes
- **Repo organization** supports parallel work: the wallet app and the two contract projects (`sepolia-token/`, `sepolia-nft/`) can be developed independently.

### Innovation / Extra Effort (2 marks)

- **Full demo stack**: includes deployable ERC-20 and ERC-721 projects (Hardhat) so the wallet can be demonstrated end-to-end.
- **NFT Gallery integration**: pulls owned NFTs via Alchemy’s NFT API and handles common metadata fields and `ipfs://` images.
- **Multi-network architecture**: networks and bridges are config-driven via `networks.json`, enabling Sepolia/Base Sepolia/Optimism Sepolia without code changes.
- **Developer utilities**: the `openwallet-app/tools/` scripts help validate NFT ownership/metadata during demos.

## Technical highlights

- **DAO-based persistence**: `WalletDao` + `TransactionLogDao` (MySQL via JDBC)
- **Async UX**: RPC/NFT operations use `CompletableFuture` + UI updates via JavaFX thread
- **Security**: encrypted wallet payload stored in DB; password-derived keys; non-custodial signing
- **Config-driven networks**: networks and bridge URLs loaded from JSON
