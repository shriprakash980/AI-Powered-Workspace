package com.devpilot.ai.git.dto;

import java.util.List;

public record GitStageRequest(
        List<String> paths
) {}
