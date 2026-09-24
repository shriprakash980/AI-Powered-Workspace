package com.devpilot.ai.git.dto;

import java.util.List;

public record GitStatusResponse(
        String branch,
        int ahead,
        int behind,
        boolean clean,
        List<GitFileStatusDto> files,
        int stagedCount,
        int unstagedCount
) {}
