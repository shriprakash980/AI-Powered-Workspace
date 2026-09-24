package com.devpilot.ai.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GitHubCreateRepoRequest(
        @NotBlank(message = "Repository name is required")
        @Size(max = 100, message = "Repository name cannot exceed 100 characters")
        String name,

        String description,
        boolean isPrivate,
        boolean autoInit
) {}
