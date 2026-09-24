package com.devpilot.ai.cicd.dto;

import java.util.UUID;

public record PipelineDiagnosisResponse(
        UUID runId,
        String failedStep,
        String summary,
        String likelyCause,
        String relevantFile,
        String suggestedFix
) {}
