package com.devpilot.ai.deployment.dto;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;

import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        UUID buildId,
        UUID artifactId,
        DeploymentEnvironment environment,
        DeploymentStatus status,
        String deploymentUrl,
        String runtime,
        String version,
        Integer allocatedPort,
        String containerId,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
    public DeploymentResponse(
            UUID id,
            UUID projectId,
            UUID userId,
            DeploymentEnvironment environment,
            DeploymentStatus status,
            Integer allocatedPort,
            String deploymentUrl,
            String containerId,
            Instant startedAt,
            Instant completedAt
    ) {
        this(id, projectId, userId, null, null, environment, status, deploymentUrl, "docker", "1.0", allocatedPort, containerId, startedAt, completedAt, Instant.now());
    }
}
