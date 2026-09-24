package com.devpilot.ai.git.dto;

public record GitCommitResponse(
        String commitHash,
        String shortHash,
        String branch,
        String message,
        int filesCommitted
) {}
