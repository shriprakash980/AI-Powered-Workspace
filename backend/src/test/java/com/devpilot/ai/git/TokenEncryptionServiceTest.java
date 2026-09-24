package com.devpilot.ai.git;

import com.devpilot.ai.git.security.TokenEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenEncryptionServiceTest {

    private TokenEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new TokenEncryptionService("devpilot-test-encryption-key-32chars-min");
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt GitHub personal access token")
    void testEncryptAndDecrypt() {
        String originalToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz";
        String encrypted = encryptionService.encrypt(originalToken);

        assertNotNull(encrypted);
        assertNotEquals(originalToken, encrypted);
        assertFalse(encrypted.contains("ghp_"));

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(originalToken, decrypted);
    }

    @Test
    @DisplayName("Should return null when encrypting or decrypting null/blank")
    void testNullAndBlankHandling() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.encrypt("   "));
        assertNull(encryptionService.decrypt(null));
        assertNull(encryptionService.decrypt("   "));
    }

    @Test
    @DisplayName("Should throw exception if ciphertext is tampered with (GCM authentication)")
    void testTamperedCiphertext() {
        String originalToken = "ghp_securetoken12345";
        String encrypted = encryptionService.encrypt(originalToken);

        // Tamper with payload
        char[] chars = encrypted.toCharArray();
        chars[chars.length - 2] = (chars[chars.length - 2] == 'A') ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(IllegalStateException.class, () -> encryptionService.decrypt(tampered));
    }
}
