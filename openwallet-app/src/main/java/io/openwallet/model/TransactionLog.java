package io.openwallet.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionLog {
    private int id;
    private String walletAddress;
    private String txHash;
    private BigDecimal amount;
    private String tokenSymbol;
    private String status;
    private LocalDateTime createdAt;

    public TransactionLog() {}

    public TransactionLog(String walletAddress, String txHash, BigDecimal amount, String tokenSymbol, String status) {
        this.walletAddress = walletAddress;
        this.txHash = txHash;
        this.amount = amount;
        this.tokenSymbol = tokenSymbol;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTokenSymbol() { return tokenSymbol; }
    public void setTokenSymbol(String tokenSymbol) { this.tokenSymbol = tokenSymbol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
