package com.devpilot.ai.git.dto;

import java.time.Instant;

public record GitHubStatusResponse(
        boolean connected,
        String username,
        String scopes,
        Instant connectedAt
) {}
