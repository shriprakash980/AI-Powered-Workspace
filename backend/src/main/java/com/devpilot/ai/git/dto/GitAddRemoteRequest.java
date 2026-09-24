package com.devpilot.ai.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GitAddRemoteRequest(
        String name,
        @NotBlank(message = "Repository URL cannot be empty")
        @Size(max = 500, message = "Repository URL cannot exceed 500 characters")
        String repositoryUrl
) {}
