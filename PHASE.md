JDBC + DB layer (MySQL)

Create schema openwallet_db with tables wallet_profiles, transaction_logs.
Add MySQL JDBC dependency and a DatabaseConfig class reading URL/user/pass (env or properties).
Implement ConnectionFactory (Singleton) and DAO interfaces (WalletDao, TransactionLogDao).
Provide concrete MySQL DAO classes using prepared statements, demonstrating CRUD plus exception handling.
OOP/Collections

Define interfaces (WalletRepository, BalanceProvider) with multiple implementations (in-memory vs MySQL).
Use List<WalletProfile> / Map<String, TokenMeta> to manage wallets/tokens generically.
Extend base classes (AbstractWalletService) and custom exceptions (DbAccessException, WalletNotFoundException).
Multithreading

Use ExecutorService/CompletableFuture (or JavaFX Service) for DB + RPC calls, with synchronized cache updates.

✅ FULL PROJECT PLAN (Two Phases)
Project: OpenWallet – Open-source, Non-custodial Java Crypto Wallet
Tech: Java 17+, JavaFX, Web3j, AES-GCM encryption, BIP-39, ERC-20, Sepolia RPC
🟦 PHASE 1 — CORE WALLET (50% Submission)

Goal: Implement the core non-custodial wallet, secure key management, basic UI, token viewing, and ETH transfers on Sepolia.

🔥 Deliverables for Phase 1

Wallet creation & restoration (mnemonic)

Secure encrypted keystore (password protected)

Show Ethereum address & balance

Send ETH on Sepolia

Basic JavaFX UI

README + screenshots

📌 PHASE 1 TASK BREAKDOWN

1. Project Setup (1 day)

Initialize GitHub repository

Setup Maven project + JavaFX, Web3j, BouncyCastle, Argon2 dependencies

Add initial README with project description

Copilot Prompt:

“Generate a Maven project with JavaFX, Web3j, and BouncyCastle dependencies.”

2. Wallet Core Implementation (2–3 days)
   🧩 Tasks:

Implement BIP-39 mnemonic generation

Implement BIP-44 EVM derivation path

Generate private/public key pairs

Generate Ethereum address

Store encrypted private key using AES-GCM + Argon2

Copilot Prompts:

“Create a WalletService class that can generate a 12-word BIP-39 mnemonic.”
“Implement BIP-44 derivation m/44’/60’/0’/0/0 to get Ethereum private key.”
“Write a CryptoUtil class that encrypts data using AES-GCM and Argon2id.”

3. Keystore System (1–2 days)
   🧩 Tasks:

Create keystore JSON format

Implement export (save to file)

Implement import (load + decrypt)

Copilot Prompt:

“Create a Keystore class that stores encrypted private key, salt, iv, and wallet metadata.”

4. Basic JavaFX UI (2–3 days)
   UI Screens:

Create Wallet

Import Wallet (mnemonic / keystore)

Show Address

Show Balance (ETH)

Send ETH (simple transfer form)

Copilot Prompts:

“Create a JavaFX screen with fields for mnemonic and buttons for Generate, Import, Export.”
“Make a send transaction form with fields: recipient, amount, gas, submit.”

5. RPC Integration (2 days)
   🧩 Tasks:

Connect to Sepolia RPC (Alchemy/Infura)

Fetch ETH balance

Fetch gas price

Build & sign ETH tx locally

Broadcast signed tx

Copilot Prompt:

“Use Web3j to fetch ETH balance and display it in JavaFX.”
“Write a function to build and sign a Sepolia transaction using a private key.”

6. Documentation + Submission (1 day)

Update README

Add architecture diagram

Add screenshots of UI

Provide build/run steps

Tag a “Phase 1 Release” on GitHub

🎉 PHASE 1 Final Output

A working non-custodial wallet that can:

create / import wallets

encrypt keys

display address + balance

send Sepolia ETH

🟩 PHASE 2 — ADVANCED FEATURES (Final Submission)

Goal: Implement tokens, bridging, Chainlink support, UI polish, and multi-chain architecture.

🔥 Deliverables for Phase 2

ERC-20 token support

Custom token import

Send tokens

Your own deployed ERC-20 token

Bridge integration (UI + testnet bridge)

Optional Chainlink feed integration

Full polished UI

Final docs + demo video

📌 PHASE 2 TASK BREAKDOWN

1. Add ERC-20 Token Support (3 days)
   🧩 Tasks:

Create TokenService

Read ERC-20 metadata (symbol, decimals)

Fetch balances

Send ERC-20 tokens (approve → transfer)

Token list management

Copilot Prompt:

“Create a TokenService class that reads symbol(), decimals(), and balanceOf() from any ERC-20 contract using Web3j.”

2. Deploy Your Custom ERC-20 Token (1 day)

Using Hardhat or Remix:

Create ERC-20 (OpenZeppelin)

Deploy to Sepolia

Mint test tokens

Add token to wallet

Reference implementation:

- `sepolia-token/` (see `sepolia-token/README.md`)

Copilot Prompt:

“Write a simple ERC-20 token using OpenZeppelin with mint() and constructor name/symbol.”

3. Add Cross-Chain Bridging UI (3–4 days)
   Options:

Use existing bridge UIs (Connext, Wormhole)

Or integrate SDK (simple calls)

Display instructions + transaction status

Copilot Prompt:

“Create a BridgeScreen.java with fields for source chain, target chain, token, amount, and a button that opens a bridge link.”

4. Chainlink Integration (Optional but great for grade) (2 days)

You can:

Add on-chain price fetch from Chainlink Sepolia feeds

Display real-time USD price for ETH/tokens

Copilot Prompt:

“Create a PriceService that calls Chainlink AggregatorV3Interface to fetch latestRoundData() on Sepolia.”

5. Multi-Network Architecture (2 days)

Add modular support:

Ethereum Sepolia (default)

Ethereum Mainnet (optional)

Polygon Testnet

Base Sepolia

Create configuration files:

networks.json
tokens.json

Copilot Prompt:

“Create a NetworkManager class that loads networks from JSON and provides RPC providers dynamically.”

6. Full UI Polish (2–3 days)

Add:

Dashboard

Token list view

Transaction history (via explorer API)

Settings page

Keystore management

7. Final Documentation & Polish (2 days)

Add final README with full features

Add diagrams (architecture, workflows)

Add demo video (screen recording)

Add instructions to build using Maven

Tag “v1.0 Release” on GitHub

🎉 PHASE 2 Final Output

A polished, open-source non-custodial wallet that supports:
✔ Secure key management
✔ ETH + ERC-20 tokens
✔ Your own token
✔ Bridging flow
✔ Chainlink feeds
✔ Multi-network foundations
✔ Full UI

This is a full-fledged multi-feature blockchain application — excellent for university, resume, GitHub contributions, and future development.

# OpenWallet

OpenWallet is an open-source, non-custodial Ethereum wallet built with Java 17, JavaFX, and Web3j. The project guides users through two delivery phases that cover everything from mnemonic-based key management to ERC-20 token flows and bridge integrations.

## Tech Stack

- Java 17+
- JavaFX for desktop UI
- Web3j for Ethereum interactions
- BouncyCastle + Argon2id for cryptography
- AES-GCM for private-key encryption
- Maven for builds

## Phase 1 — Core Wallet (Mid Submission)

Focus on the foundational wallet experience:

1. **Project setup** — Maven project with JavaFX/Web3j/BouncyCastle/Argon2 dependencies, initial README.
2. **Wallet core** — BIP-39 mnemonic generation, BIP-44 derivation (m/44'/60'/0'/0/0), key pair + address creation, AES-GCM + Argon2 keystore encryption.
3. **Keystore system** — Export/import encrypted JSON keystores.
4. **JavaFX UI** — Screens for creating/importing wallets, showing address/balance, initiating sends.
5. **RPC integration** — Connect to Sepolia, fetch balances/gas, sign and send ETH transactions.
6. **Docs & release** — README updates, architecture notes, screenshots, Phase 1 tag.

Phase 1 output: create/import wallets, encrypted key storage, address & balance view, send Sepolia ETH, basic desktop UI.

## Phase 2 — Advanced Features (Final Submission)

Add power-user capabilities and polish:

1. **ERC-20 support** — Token metadata, balances, transfers, custom token management.
2. **Custom token** — Deploy an ERC-20 on Sepolia (OpenZeppelin/Hardhat) and integrate it.
3. **Bridging UI** — Screens/workflows for cross-chain transfers (link to Connext, Wormhole, etc.).
4. **Chainlink integration** (optional) — Fetch USD prices via Sepolia data feeds.
5. **Multi-network architecture** — Config-driven networks (Sepolia, Mainnet, Polygon, Base) via `networks.json` & `tokens.json`.
6. **UI polish** — Dashboard, token list, transaction history, settings, keystore management.
7. **Docs & release** — Final README, diagrams, demo video, v1.0 tag.

Phase 2 output: production-ready wallet with secure key management, ETH + ERC-20 support, custom token, bridging flow, price feeds, multi-network backbone, and polished UI.

## Repository Layout

```
openwallet-app/
  pom.xml
  src/main/java/io/openwallet/MainApp.java
  src/main/resources/
  src/test/java/io/openwallet/MainAppTest.java
```

## Getting Started

1. Install Java 17 and Maven 3.9+.
2. Navigate into the project folder:
   ```
   cd openwallet-app
   ```
3. Run the JavaFX app:
   ```
   mvn javafx:run
   ```

## Next Steps

- Flesh out wallet domain modules (WalletService, CryptoUtil, KeystoreService).
- Bootstrap JavaFX views for wallet creation/import.
- Integrate Sepolia RPC credentials via environment variables or config files.
- Document security considerations (password strength, storage, network calls).

Java GUI-Based Projects Marking Rubric - Review 1

OOP Implementation (Polymorphism, Inheritance, Exception Handling, Interfaces) - 10 marks
Collections & Generics - 6 mark
Multithreading & Synchronization - 4 mark
Classes for the database operations - 7 mark
Database Connectivity (JDBC) - 3 mark
Implement JDBC for database connectivity - 3 mark
