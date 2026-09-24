package com.devpilot.ai.cicd.controller;

import com.devpilot.ai.cicd.service.PipelineTriggerService;
import com.devpilot.ai.cicd.service.WebhookVerificationService;
import com.devpilot.ai.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/webhooks/github")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final WebhookVerificationService verificationService;
    private final PipelineTriggerService triggerService;

    public GitHubWebhookController(
            WebhookVerificationService verificationService,
            PipelineTriggerService triggerService) {
        this.verificationService = verificationService;
        this.triggerService = triggerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false, defaultValue = "push") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody(required = false) byte[] rawPayload) {

        if (rawPayload == null || rawPayload.length == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Empty webhook payload body"));
        }

        // Verify HMAC-SHA256 signature if webhook secret is configured
        if (verificationService.isVerificationEnabled()) {
            if (!verificationService.verifySignature(rawPayload, signatureHeader)) {
                log.warn("Unauthorized GitHub webhook request: HMAC signature verification failed.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or missing X-Hub-Signature-256 header"));
            }
        }

        // Duplicate delivery protection
        if (deliveryId != null && verificationService.isDuplicateDelivery(deliveryId)) {
            log.info("Duplicate delivery ID received: {}. Skipping execution.", deliveryId);
            return ResponseEntity.ok(ApiResponse.success("Duplicate delivery ignored", "Event processed"));
        }

        String payloadStr = new String(rawPayload, StandardCharsets.UTF_8);
        triggerService.processGitHubWebhookEvent(eventType, deliveryId, payloadStr);

        return ResponseEntity.ok(ApiResponse.success("Webhook event processed successfully", "GitHub webhook event queued"));
    }
}
