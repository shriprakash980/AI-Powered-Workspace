package com.devpilot.ai.deployment.dto;

import com.devpilot.ai.build.entity.enums.LogType;

import java.time.Instant;
import java.util.UUID;

public record DeploymentLogDto(
        UUID id,
        UUID deploymentId,
        LogType logType,
        String message,
        Integer sequenceNumber,
        Instant createdAt
) {}
