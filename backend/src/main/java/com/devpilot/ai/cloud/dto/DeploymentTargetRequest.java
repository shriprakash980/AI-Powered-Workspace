package com.devpilot.ai.cloud.dto;

import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeploymentTargetRequest(
        @NotBlank(message = "Target name is required")
        String name,

        @NotNull(message = "Cloud provider type is required")
        CloudProviderType provider,

        @NotNull(message = "Environment is required")
        DeploymentEnvironment environment,

        UUID credentialId,
        String region,
        String customDomain,
        String configurationJson
) {}
