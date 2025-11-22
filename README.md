# OpenWallet

OpenWallet is a secure, open-source, non-custodial Ethereum wallet built with **Java 17**, **JavaFX**, and **Web3j**. It is designed to provide a seamless experience for managing Ethereum assets on the Sepolia Testnet, featuring military-grade encryption, local database storage, and a modern responsive UI.

## 🚀 Features

### Core Wallet Features

- **Non-Custodial**: You own your keys. Private keys are encrypted and stored locally.
- **BIP-39 Compliant**: Generates standard 12-word mnemonic phrases for wallet recovery.
- **Secure Login**: Password-protected access using AES-GCM encryption.
- **Real-time Balance**: Fetches live ETH balances from the Sepolia Testnet via Alchemy RPC.
- **Send & Receive**: Easily send ETH to any address and receive funds via QR code.
- **Transaction History**: Automatically logs all outgoing transactions to a local database.

### Technical Highlights (University Rubric Compliance)

- **OOP Principles**: Extensive use of Interfaces (`WalletDao`, `TransactionLogDao`), Inheritance (`OpenWalletException`), and Polymorphism.
- **Database Connectivity**: Custom JDBC implementation with MySQL for storing encrypted wallet profiles and transaction logs.
- **Multithreading**: Uses `CompletableFuture` for non-blocking blockchain RPC calls and `Platform.runLater` for thread-safe UI updates.
- **Collections**: Utilizes `List`, `Optional`, and `ObservableList` for efficient data management.
- **Security**: Implements PBKDF2 key derivation and AES-256-GCM encryption.

## 🛠 Tech Stack

- **Language**: Java 17
- **UI Framework**: JavaFX 21 (FXML + CSS)
- **Blockchain Library**: Web3j 4.10.3
- **Database**: MySQL 8.0 (JDBC)
- **Build Tool**: Maven
- **Cryptography**: Bouncy Castle, PBKDF2, AES-GCM
- **Utilities**: ZXing (QR Code Generation)

## 📦 Installation & Setup

### Prerequisites

1.  **Java JDK 17** or higher.
2.  **Maven 3.9+**.
3.  **MySQL Server** running locally.

### Database Setup

1.  Open your MySQL client (Workbench or Command Line).
2.  Create the database and tables using the provided schema:

    ```sql
    CREATE DATABASE openwallet_db;
    USE openwallet_db;

    CREATE TABLE wallet_profiles (
        id INT AUTO_INCREMENT PRIMARY KEY,
        profile_name VARCHAR(255) UNIQUE NOT NULL,
        wallet_address VARCHAR(42) NOT NULL,
        encrypted_json TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE transaction_logs (
        id INT AUTO_INCREMENT PRIMARY KEY,
        wallet_address VARCHAR(42) NOT NULL,
        tx_hash VARCHAR(66) NOT NULL,
        amount DECIMAL(20, 8) NOT NULL,
        token_symbol VARCHAR(10) DEFAULT 'ETH',
        status VARCHAR(20) DEFAULT 'PENDING',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    ```

3.  Update `src/main/resources/db.properties` with your MySQL credentials:
    ```properties
    db.url=jdbc:mysql://localhost:3306/openwallet_db
    db.user=root
    db.password=YOUR_PASSWORD
    rpc.url=https://eth-sepolia.g.alchemy.com/v2/YOUR_API_KEY
    ```

### Running the Application

1.  Clone the repository:
    ```bash
    git clone https://github.com/Hellkryptonium/OpenWallet.git
    cd OpenWallet/openwallet-app
    ```
2.  Build and Run:
    ```bash
    mvn javafx:run
    ```

## 🖥 Usage Guide

1.  **Startup**: On first launch, click **"Create New Wallet"**.
2.  **Creation**:
    - Write down your **12-word Secret Recovery Phrase**. This is the ONLY way to recover your funds.
    - Set a strong password to encrypt your wallet file.
3.  **Dashboard**:
    - View your **Wallet Address** and **ETH Balance**.
    - **Receive**: Click "Receive" to show your QR code.
    - **Send**: Click "Send", enter a recipient address (0x...) and amount.
4.  **History**: View your recent transactions in the table at the bottom of the dashboard.
5.  **Logout**: Securely lock your wallet when done.

## 📂 Project Structure

```
openwallet-app/
├── src/main/java/io/openwallet/
│   ├── controller/       # JavaFX Controllers (MVC Pattern)
│   ├── crypto/           # Encryption & Key Derivation Logic
│   ├── db/               # DAO Layer & Database Connection (Singleton)
│   ├── exception/        # Custom Exception Classes
│   ├── model/            # POJOs (WalletProfile, TransactionLog)
│   ├── service/          # Business Logic (Web3j, BIP-39)
│   └── MainApp.java      # Application Entry Point
├── src/main/resources/
│   ├── io/openwallet/view/  # FXML Views & CSS Styles
│   └── db.properties        # Configuration File
└── pom.xml                  # Maven Dependencies
```

## 🔒 Security Architecture

- **Key Storage**: Private keys are never stored in plain text. They are encrypted using **AES-GCM** with a key derived from your password using **PBKDF2WithHmacSHA256**.
- **Non-Blocking UI**: All network operations (RPC calls) run on background threads to prevent UI freezing, ensuring a smooth user experience.

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.
