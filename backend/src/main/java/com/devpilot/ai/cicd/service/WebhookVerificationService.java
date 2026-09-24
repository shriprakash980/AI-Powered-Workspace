package com.devpilot.ai.cicd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebhookVerificationService {

    private static final Logger log = LoggerFactory.getLogger(WebhookVerificationService.class);

    private final String webhookSecret;
    private final Set<String> processedDeliveryIds = ConcurrentHashMap.newKeySet();
    private static final int MAX_DELIVERY_CACHE_SIZE = 1000;

    public WebhookVerificationService(@Value("${app.github.webhook-secret:${GITHUB_WEBHOOK_SECRET:}}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isVerificationEnabled() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public boolean verifySignature(byte[] payload, String signatureHeader) {
        if (!isVerificationEnabled()) {
            log.warn("GitHub webhook secret is not configured. Webhook verification is running in open dev mode.");
            return true;
        }

        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.warn("Missing or invalid X-Hub-Signature-256 header format");
            return false;
        }

        String expectedHex = signatureHeader.substring("sha256=".length()).trim();

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] computedHash = mac.doFinal(payload);
            String computedHex = HexFormat.of().formatHex(computedHash);

            boolean match = MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    expectedHex.getBytes(StandardCharsets.UTF_8)
            );

            if (!match) {
                log.warn("Webhook signature mismatch for incoming GitHub event");
            }
            return match;
        } catch (Exception e) {
            log.error("Failed to compute HMAC SHA-256 webhook signature: {}", e.getMessage());
            return false;
        }
    }

    public boolean isDuplicateDelivery(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) {
            return false;
        }
        if (processedDeliveryIds.contains(deliveryId)) {
            return true;
        }
        if (processedDeliveryIds.size() > MAX_DELIVERY_CACHE_SIZE) {
            processedDeliveryIds.clear();
        }
        processedDeliveryIds.add(deliveryId);
        return false;
    }
}
