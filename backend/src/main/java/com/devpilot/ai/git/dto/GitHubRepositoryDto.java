package com.devpilot.ai.git.dto;

public record GitHubRepositoryDto(
        Long id,
        String name,
        String fullName,
        boolean isPrivate,
        String defaultBranch,
        String htmlUrl,
        String cloneUrl,
        String description,
        String updatedAt
) {}
