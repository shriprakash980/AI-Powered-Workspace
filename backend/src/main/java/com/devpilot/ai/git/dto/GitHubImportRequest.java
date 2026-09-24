package com.devpilot.ai.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GitHubImportRequest(
        @NotBlank(message = "GitHub repository (owner/repo or URL) is required")
        String repository,

        @NotBlank(message = "Project name is required")
        @Size(max = 120, message = "Project name cannot exceed 120 characters")
        String projectName,

        String description,
        String template
) {}
