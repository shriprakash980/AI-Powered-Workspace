package com.devpilot.ai.cicd.dto;

import java.util.List;
import java.util.UUID;

public record PipelineLogResponse(
        UUID runId,
        String stepName,
        List<PipelineLogDto> logs
) {}
