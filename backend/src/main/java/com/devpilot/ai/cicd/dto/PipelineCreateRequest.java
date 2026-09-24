package com.devpilot.ai.cicd.dto;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PipelineCreateRequest(
        @NotBlank(message = "Pipeline name is required")
        @Size(max = 120, message = "Pipeline name cannot exceed 120 characters")
        String name,

        String description,
        Boolean enabled,
        String configPath,
        String defaultBranch,
        Boolean triggerOnPush,
        Boolean triggerOnPullRequest,
        Boolean allowManualTrigger,
        Boolean autoDeployEnabled,
        DeploymentEnvironment deploymentEnvironment
) {}
