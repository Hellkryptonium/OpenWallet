package io.openwallet.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {

    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int SALT_LENGTH = 16;

    // Argon2 parameters
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_MEMORY = 65536;
    private static final int ARGON2_PARALLELISM = 1;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String encrypt(String plainText, String password) throws Exception {
        // 1. Generate Salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // 2. Derive Key using Argon2
        SecretKey key = deriveKey(password, salt);

        // 3. Generate IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 4. Encrypt using AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 5. Construct EncryptedData object
        EncryptedData data = new EncryptedData(
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(cipherText)
        );

        return objectMapper.writeValueAsString(data);
    }

    public static String decrypt(String json, String password) throws Exception {
        // 1. Parse JSON
        EncryptedData data = objectMapper.readValue(json, EncryptedData.class);

        byte[] salt = Base64.getDecoder().decode(data.getSalt());
        byte[] iv = Base64.getDecoder().decode(data.getIv());
        byte[] cipherText = Base64.getDecoder().decode(data.getCipherText());

        // 2. Derive Key
        SecretKey key = deriveKey(password, salt);

        // 3. Decrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String password, byte[] salt) {
        Argon2 argon2 = Argon2Factory.create();
        try {
            
            return deriveKeyPBKDF2(password, salt);
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }

    private static SecretKey deriveKeyPBKDF2(String password, byte[] salt) {
        try {
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            java.security.spec.KeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 65536, AES_KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (Exception e) {
            throw new RuntimeException("Error deriving key", e);
        }
    }
}
