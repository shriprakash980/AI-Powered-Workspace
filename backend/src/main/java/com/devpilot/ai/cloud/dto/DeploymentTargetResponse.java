package com.devpilot.ai.cloud.dto;

import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.model.CloudTargetStatus;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;

import java.time.Instant;
import java.util.UUID;

public record DeploymentTargetResponse(
        UUID id,
        UUID projectId,
        String name,
        CloudProviderType provider,
        DeploymentEnvironment environment,
        CloudTargetStatus status,
        String region,
        String customDomain,
        boolean approvalRequired,
        boolean approved,
        Instant lastHealthCheckAt,
        Instant createdAt
) {}
