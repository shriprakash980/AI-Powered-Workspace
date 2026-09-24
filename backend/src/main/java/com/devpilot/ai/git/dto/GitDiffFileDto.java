package com.devpilot.ai.git.dto;

public record GitDiffFileDto(
        String path,
        String oldPath,
        String changeType, // ADD, MODIFY, DELETE, RENAME
        String diff,
        int additions,
        int deletions
) {}
