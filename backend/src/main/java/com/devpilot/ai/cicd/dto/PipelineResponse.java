package com.devpilot.ai.cicd.dto;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;

import java.time.Instant;
import java.util.UUID;

public record PipelineResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        boolean enabled,
        String configPath,
        String defaultBranch,
        boolean triggerOnPush,
        boolean triggerOnPullRequest,
        boolean allowManualTrigger,
        boolean autoDeployEnabled,
        DeploymentEnvironment deploymentEnvironment,
        Instant createdAt,
        Instant updatedAt
) {}
