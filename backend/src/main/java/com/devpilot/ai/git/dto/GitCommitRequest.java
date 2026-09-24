package com.devpilot.ai.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GitCommitRequest(
        @NotBlank(message = "Commit message cannot be empty")
        @Size(max = 1000, message = "Commit message cannot exceed 1000 characters")
        String message
) {}
