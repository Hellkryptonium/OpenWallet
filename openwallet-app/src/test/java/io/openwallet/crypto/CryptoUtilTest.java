package io.openwallet.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    void testEncryptionDecryption() throws Exception {
        String originalText = "super_secret_private_key";
        String password = "strong_password";

        String encryptedJson = CryptoUtil.encrypt(originalText, password);
        assertNotNull(encryptedJson);
        assertNotEquals(originalText, encryptedJson);

        String decryptedText = CryptoUtil.decrypt(encryptedJson, password);
        assertEquals(originalText, decryptedText);
    }

    @Test
    void testDecryptionFailsWithWrongPassword() throws Exception {
        String originalText = "data";
        String password = "pass";
        String wrongPassword = "wrong";

        String encryptedJson = CryptoUtil.encrypt(originalText, password);
        
        assertThrows(Exception.class, () -> {
            CryptoUtil.decrypt(encryptedJson, wrongPassword);
        });
    }
}
