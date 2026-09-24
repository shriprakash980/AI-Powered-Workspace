package com.devpilot.ai.git.dto;

public record GitPushResponse(
        String branch,
        String remote,
        int commitsPushed,
        String remoteStatus,
        String message
) {}
