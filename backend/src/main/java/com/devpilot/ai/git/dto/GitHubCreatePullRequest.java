package com.devpilot.ai.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GitHubCreatePullRequest(
        @NotBlank(message = "Pull request title is required")
        @Size(max = 250, message = "Pull request title cannot exceed 250 characters")
        String title,

        String body,

        @NotBlank(message = "Head branch is required")
        String head,

        @NotBlank(message = "Base branch is required")
        String base
) {}
