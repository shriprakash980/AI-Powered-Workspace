package com.devpilot.ai.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GitCreateBranchRequest(
        @NotBlank(message = "Branch name cannot be empty")
        @Size(max = 100, message = "Branch name cannot exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+(?:/[a-zA-Z0-9_.-]+)*$", message = "Invalid Git branch name format")
        String name
) {}
