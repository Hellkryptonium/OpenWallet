CREATE DATABASE IF NOT EXISTS openwallet_db;
USE openwallet_db;

CREATE TABLE IF NOT EXISTS wallet_profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    profile_name VARCHAR(255) NOT NULL,
    wallet_address VARCHAR(42) NOT NULL,
    encrypted_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transaction_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_address VARCHAR(42) NOT NULL,
    tx_hash VARCHAR(66) NOT NULL,
    amount DECIMAL(30, 18),
    token_symbol VARCHAR(10),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
