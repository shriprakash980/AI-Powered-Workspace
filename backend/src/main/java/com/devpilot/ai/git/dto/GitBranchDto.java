package com.devpilot.ai.git.dto;

public record GitBranchDto(
        String name,
        boolean current,
        boolean remote,
        String commitHash
) {}
