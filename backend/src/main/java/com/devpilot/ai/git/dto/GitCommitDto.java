package com.devpilot.ai.git.dto;

import java.time.Instant;
import java.util.List;

public record GitCommitDto(
        String commitHash,
        String shortHash,
        String message,
        String authorName,
        String authorEmail,
        Instant timestamp,
        List<String> parentHashes
) {}
