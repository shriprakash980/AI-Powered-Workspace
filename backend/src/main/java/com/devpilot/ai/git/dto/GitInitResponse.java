package com.devpilot.ai.git.dto;

import java.util.UUID;

public record GitInitResponse(
        UUID projectId,
        boolean initialized,
        String defaultBranch,
        String message
) {}
