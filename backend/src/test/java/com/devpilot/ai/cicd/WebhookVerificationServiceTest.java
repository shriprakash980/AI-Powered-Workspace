package com.devpilot.ai.cicd;

import com.devpilot.ai.cicd.service.WebhookVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class WebhookVerificationServiceTest {

    @Test
    @DisplayName("Should verify valid HMAC SHA-256 webhook signature")
    void testValidSignature() throws Exception {
        String secret = "test-webhook-secret-123";
        WebhookVerificationService service = new WebhookVerificationService(secret);

        byte[] payload = "{\"ref\":\"refs/heads/main\",\"after\":\"8f92ab1\"}".getBytes(StandardCharsets.UTF_8);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signatureHex = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));

        assertTrue(service.verifySignature(payload, signatureHex));
    }

    @Test
    @DisplayName("Should reject invalid signature")
    void testInvalidSignature() {
        String secret = "test-webhook-secret-123";
        WebhookVerificationService service = new WebhookVerificationService(secret);

        byte[] payload = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        assertFalse(service.verifySignature(payload, "sha256=invalidhash123456789"));
    }

    @Test
    @DisplayName("Should correctly track duplicate delivery IDs")
    void testDuplicateDeliveryTracking() {
        WebhookVerificationService service = new WebhookVerificationService("secret");
        String deliveryId = "deliv-uuid-101";

        assertFalse(service.isDuplicateDelivery(deliveryId));
        assertTrue(service.isDuplicateDelivery(deliveryId));
    }
}
