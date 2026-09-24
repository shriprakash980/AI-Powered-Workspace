package com.devpilot.ai.build.dto;

import com.devpilot.ai.build.entity.enums.BuildMode;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.entity.enums.ProjectType;

import java.time.Instant;
import java.util.UUID;

public record BuildResponse(
        UUID buildId,
        UUID projectId,
        UUID userId,
        BuildStatus status,
        ProjectType projectType,
        BuildMode mode,
        String command,
        String commitHash,
        String artifactPath,
        Integer exitCode,
        Long durationMs,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {}
