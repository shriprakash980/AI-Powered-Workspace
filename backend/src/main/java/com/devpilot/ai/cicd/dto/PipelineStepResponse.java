package com.devpilot.ai.cicd.dto;

import com.devpilot.ai.cicd.model.PipelineStepStatus;
import com.devpilot.ai.cicd.model.PipelineStepType;

import java.time.Instant;
import java.util.UUID;

public record PipelineStepResponse(
        UUID id,
        UUID pipelineRunId,
        String stepName,
        PipelineStepType stepType,
        int stepOrder,
        PipelineStepStatus status,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        Integer exitCode,
        String errorMessage,
        Instant createdAt
) {}
