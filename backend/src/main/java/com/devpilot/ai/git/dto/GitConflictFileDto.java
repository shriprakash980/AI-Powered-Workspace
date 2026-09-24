package com.devpilot.ai.git.dto;

public record GitConflictFileDto(
        String path,
        boolean conflictMarkers
) {}
