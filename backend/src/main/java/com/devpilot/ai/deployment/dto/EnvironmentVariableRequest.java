package com.devpilot.ai.deployment.dto;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EnvironmentVariableRequest(
        @NotBlank(message = "Variable name is required")
        @Pattern(regexp = "^[A-Za-z_][A-Za-z0-9_]*$", message = "Invalid environment variable name")
        String name,
        @NotBlank(message = "Variable value is required")
        String value,
        @NotNull(message = "Environment is required")
        DeploymentEnvironment environment
) {}
