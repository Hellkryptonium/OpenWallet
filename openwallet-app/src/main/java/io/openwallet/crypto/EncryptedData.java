package io.openwallet.crypto;

public class EncryptedData {
    private String salt;
    private String iv;
    private String cipherText;
    private String mac; // Optional, GCM handles auth tag usually appended to cipherText, but explicit field can be useful if separated.
                        // For standard Java GCM, tag is appended. We'll stick to salt, iv, cipherText (Base64 encoded).

    public EncryptedData() {}

    public EncryptedData(String salt, String iv, String cipherText) {
        this.salt = salt;
        this.iv = iv;
        this.cipherText = cipherText;
    }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public String getCipherText() { return cipherText; }
    public void setCipherText(String cipherText) { this.cipherText = cipherText; }
}
