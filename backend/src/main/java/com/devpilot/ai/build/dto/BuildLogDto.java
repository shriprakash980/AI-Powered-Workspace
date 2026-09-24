package com.devpilot.ai.build.dto;

import com.devpilot.ai.build.entity.enums.LogType;

import java.time.Instant;
import java.util.UUID;

public record BuildLogDto(
        UUID id,
        UUID buildId,
        LogType logType,
        String message,
        Integer sequenceNumber,
        Instant createdAt
) {}
