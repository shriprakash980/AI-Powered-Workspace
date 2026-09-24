package com.devpilot.ai.cicd.dto;

public record PipelineLogDto(
        String timestamp,
        String stepName,
        String stream,
        String message
) {}
