package com.devpilot.ai.deployment.dto;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeploymentRequest(
        UUID buildId,
        UUID artifactId,
        @NotNull(message = "Environment is required")
        DeploymentEnvironment environment,
        String customRuntime
) {
    public DeploymentRequest(DeploymentEnvironment environment, String customRuntime) {
        this(null, null, environment, customRuntime);
    }
}
