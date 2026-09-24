package com.devpilot.ai.git.dto;

import java.util.List;

public record GitPullResponse(
        String status, // SUCCESS, UP_TO_DATE, FAST_FORWARD, MERGED, CONFLICTED
        List<GitConflictFileDto> files,
        int commitsReceived,
        String message
) {}
