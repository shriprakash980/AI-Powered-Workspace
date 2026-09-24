package com.devpilot.ai.cicd.worker;

import java.util.UUID;

public record PipelineJob(
        UUID runId,
        UUID projectId,
        UUID pipelineId,
        boolean isFork
) {}
