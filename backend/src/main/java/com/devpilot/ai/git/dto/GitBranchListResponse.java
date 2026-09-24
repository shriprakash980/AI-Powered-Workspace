package com.devpilot.ai.git.dto;

import java.util.List;

public record GitBranchListResponse(
        String currentBranch,
        List<GitBranchDto> branches
) {}
