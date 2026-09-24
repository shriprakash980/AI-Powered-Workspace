package com.devpilot.ai.git.dto;

public record GitHubPullRequestDto(
        int number,
        String title,
        String body,
        String state,
        String htmlUrl,
        String headBranch,
        String baseBranch,
        String author,
        String createdAt
) {}
