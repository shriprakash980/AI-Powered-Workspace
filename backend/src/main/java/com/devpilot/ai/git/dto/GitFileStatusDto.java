package com.devpilot.ai.git.dto;

public record GitFileStatusDto(
        String path,
        String status, // MODIFIED, ADDED, DELETED, RENAMED, UNTRACKED, CONFLICTED
        boolean staged,
        boolean conflictMarkers
) {}
