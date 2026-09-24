package com.devpilot.ai.cicd.dto;

import com.devpilot.ai.cicd.model.PipelineRunStatus;
import com.devpilot.ai.cicd.model.PipelineTriggerType;

import java.time.Instant;
import java.util.UUID;

public record PipelineRunResponse(
        UUID id,
        UUID pipelineId,
        UUID projectId,
        String commitSha,
        String branch,
        String commitMessage,
        PipelineTriggerType triggerType,
        String triggeredBy,
        PipelineRunStatus status,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        String errorMessage,
        UUID artifactId,
        UUID deploymentId,
        boolean isFork,
        Instant createdAt
) {}
