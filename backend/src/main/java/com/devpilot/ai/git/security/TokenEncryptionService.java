package com.devpilot.ai.git.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(TokenEncryptionService.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public TokenEncryptionService(@Value("${app.github.encryption-key:devpilot-secret-key-32chars-min-aes256}") String rawKey) {
        this.secureRandom = new SecureRandom();
        this.secretKey = deriveKey(rawKey);
    }

    private SecretKey deriveKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive AES-256 encryption key", e);
        }
    }

    /**
     * Encrypts plaintext token using AES-256-GCM.
     * The resulting payload is Base64 encoded: [12-byte IV] + [Ciphertext with GCM Auth Tag].
     */
    public String encrypt(String plaintextToken) {
        if (plaintextToken == null || plaintextToken.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plaintextToken.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt OAuth token: security cipher error");
            throw new IllegalStateException("Failed to securely encrypt token at rest", e);
        }
    }

    /**
     * Decrypts ciphertext back to the original plaintext token.
     * Throws an exception if ciphertext has been modified or corrupted (GCM authentication check).
     */
    public String decrypt(String encryptedTokenBase64) {
        if (encryptedTokenBase64 == null || encryptedTokenBase64.isBlank()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedTokenBase64);
            if (decoded.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted payload size");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt OAuth token: authentication or payload corruption error");
            throw new IllegalStateException("Failed to securely decrypt token", e);
        }
    }
}
