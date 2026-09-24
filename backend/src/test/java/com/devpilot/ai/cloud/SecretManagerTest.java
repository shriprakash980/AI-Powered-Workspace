package com.devpilot.ai.cloud;

import com.devpilot.ai.cloud.service.SecretManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretManagerTest {

    private SecretManager secretManager;

    @BeforeEach
    void setUp() {
        secretManager = new SecretManager("TestMasterKeyForAES256GCMEncryption2026!");
    }

    @Test
    void testEncryptAndDecryptSuccess() {
        String original = "AKIAIOSFODNN7EXAMPLE_SECRET_KEY";
        String encrypted = secretManager.encrypt(original);

        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = secretManager.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testNullOrEmptyStringHandling() {
        assertNull(secretManager.encrypt(null));
        assertEquals("", secretManager.encrypt(""));
        assertNull(secretManager.decrypt(null));
        assertEquals("", secretManager.decrypt(""));
    }
}
