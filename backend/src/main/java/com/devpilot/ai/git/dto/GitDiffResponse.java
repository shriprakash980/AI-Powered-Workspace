package com.devpilot.ai.git.dto;

import java.util.List;

public record GitDiffResponse(
        List<GitDiffFileDto> files,
        int totalAdditions,
        int totalDeletions
) {}
