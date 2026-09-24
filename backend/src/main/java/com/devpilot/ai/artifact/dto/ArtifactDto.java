package com.devpilot.ai.artifact.dto;

import com.devpilot.ai.artifact.entity.enums.ArtifactType;

import java.time.Instant;
import java.util.UUID;

public record ArtifactDto(
        UUID id,
        UUID projectId,
        UUID buildId,
        ArtifactType artifactType,
        String storageKey,
        Long sizeBytes,
        String checksum,
        Instant createdAt
) {}
