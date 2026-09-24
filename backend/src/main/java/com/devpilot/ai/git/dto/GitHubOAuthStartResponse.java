package com.devpilot.ai.git.dto;

public record GitHubOAuthStartResponse(
        String authorizationUrl,
        String state
) {}
