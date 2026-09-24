package com.devpilot.ai.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GitCheckoutRequest(
        @NotBlank(message = "Branch name cannot be empty")
        @Size(max = 100, message = "Branch name cannot exceed 100 characters")
        String branch,
        boolean createIfNotExists
) {}
