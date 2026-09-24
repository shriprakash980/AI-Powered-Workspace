package com.devpilot.ai.deployment.dto;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;

import java.time.Instant;
import java.util.UUID;

public record EnvironmentVariableResponse(
        UUID id,
        UUID projectId,
        String name,
        String maskedValue,
        DeploymentEnvironment environment,
        Instant createdAt,
        Instant updatedAt
) {}
