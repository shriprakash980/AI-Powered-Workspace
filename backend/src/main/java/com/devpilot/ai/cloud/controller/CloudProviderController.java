package com.devpilot.ai.cloud.controller;

import com.devpilot.ai.cloud.dto.CloudCredentialRequest;
import com.devpilot.ai.cloud.dto.CloudCredentialResponse;
import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.service.CloudDeploymentTargetService;
import com.devpilot.ai.cloud.service.CloudProviderFactory;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cloud")
public class CloudProviderController {

    private final CloudProviderFactory providerFactory;
    private final CloudDeploymentTargetService cloudService;

    public CloudProviderController(CloudProviderFactory providerFactory,
                                   CloudDeploymentTargetService cloudService) {
        this.providerFactory = providerFactory;
        this.cloudService = cloudService;
    }

    private UUID resolveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }

    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<CloudProviderType>>> getAvailableProviders() {
        List<CloudProviderType> providers = providerFactory.getAvailableProviderTypes();
        return ResponseEntity.ok(ApiResponse.success(providers, "Available cloud providers retrieved successfully"));
    }

    @PostMapping("/credentials")
    public ResponseEntity<ApiResponse<CloudCredentialResponse>> addCredential(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CloudCredentialRequest request) {

        UUID userId = resolveUserId(principal);
        CloudCredentialResponse response = cloudService.createCredential(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cloud credentials saved securely"));
    }

    @GetMapping("/credentials")
    public ResponseEntity<ApiResponse<List<CloudCredentialResponse>>> getCredentials(
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = resolveUserId(principal);
        List<CloudCredentialResponse> credentials = cloudService.getUserCredentials(userId);
        return ResponseEntity.ok(ApiResponse.success(credentials, "User cloud credentials retrieved successfully"));
    }
}
