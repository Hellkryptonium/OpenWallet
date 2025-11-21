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
