package com.devpilot.ai.build.dto;

import com.devpilot.ai.build.entity.enums.BuildMode;
import jakarta.validation.constraints.NotNull;

public record BuildRequest(
        @NotNull(message = "Build mode is required")
        BuildMode mode,
        String customCommand
) {}
