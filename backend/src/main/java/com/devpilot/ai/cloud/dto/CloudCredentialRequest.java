package com.devpilot.ai.cloud.dto;

import com.devpilot.ai.cloud.model.CloudProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CloudCredentialRequest(
        @NotBlank(message = "Credential name is required")
        String name,

        @NotNull(message = "Cloud provider type is required")
        CloudProviderType provider,

        String accessKey,
        String secretKey,
        String region,
        String metadataJson
) {}
