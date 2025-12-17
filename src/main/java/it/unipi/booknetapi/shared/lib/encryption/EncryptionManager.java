package it.unipi.booknetapi.shared.lib.encryption;

import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionManager {

    private final PasswordEncoder passwordEncoder; // BCrypt
    private final SecretKey aesKey; // AES Key

    // Constants for AES-GCM
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 12 bytes IV
    private static final int GCM_TAG_LENGTH = 128; // 128 bit auth tag

    public EncryptionManager(AppConfig appConfig) {
        // 1. Initialize BCrypt for passwords
        this.passwordEncoder = new BCryptPasswordEncoder();

        // 2. Initialize AES Key from Config
        // We ensure the key is valid bytes
        byte[] keyBytes = appConfig.getEncryptionKey().getBytes(StandardCharsets.UTF_8);
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }


    // ==========================================
    //  PART 1: PASSWORD HASHING (One-Way)
    // ==========================================

    /**
     * Hashes a raw password using BCrypt.
     * Use this when creating a new user.
     */
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Checks if a raw password matches the hashed storage.
     * Use this during Login.
     */
    public boolean checkPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }


    // ==========================================
    //  PART 2: DATA ENCRYPTION (Two-Way AES)
    // ==========================================

    /**
     * Encrypts a string (e.g., Credit Card, Phone Number).
     * Returns: Base64 string containing [IV + EncryptedData]
     */
    public String encrypt(String plainData) {
        try {
            // 1. Generate random IV (Nonce)
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. Initialize Cipher
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

            // 3. Encrypt
            byte[] cipherText = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));

            // 4. Combine IV + CipherText (we need IV to decrypt later)
            byte[] output = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(cipherText, 0, output, iv.length, cipherText.length);

            // 5. Return as Base64 String
            return Base64.getEncoder().encodeToString(output);

        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }


    /**
     * Decrypts a Base64 string back to plain text.
     */
    public String decrypt(String encryptedData) {
        try {
            // 1. Decode Base64
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            // 2. Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);

            // 3. Extract CipherText
            int cipherTextSize = decoded.length - GCM_IV_LENGTH;
            byte[] cipherText = new byte[cipherTextSize];
            System.arraycopy(decoded, GCM_IV_LENGTH, cipherText, 0, cipherTextSize);

            // 4. Decrypt
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }

}