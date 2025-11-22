package io.openwallet.model;

import java.time.LocalDateTime;

public class WalletProfile {
    private int id;
    private String profileName;
    private String walletAddress;
    private String encryptedJson;
    private LocalDateTime createdAt;

    public WalletProfile() {}

    public WalletProfile(String profileName, String walletAddress, String encryptedJson) {
        this.profileName = profileName;
        this.walletAddress = walletAddress;
        this.encryptedJson = encryptedJson;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }

    public String getEncryptedJson() { return encryptedJson; }
    public void setEncryptedJson(String encryptedJson) { this.encryptedJson = encryptedJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
