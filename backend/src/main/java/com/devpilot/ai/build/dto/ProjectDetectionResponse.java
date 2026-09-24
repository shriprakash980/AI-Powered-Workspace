package com.devpilot.ai.build.dto;

import com.devpilot.ai.build.entity.enums.ProjectType;

import java.util.List;

public record ProjectDetectionResponse(
        List<ProjectType> detectedTypes,
        ProjectType primaryType,
        String suggestedBuildCommand,
        String suggestedTestCommand,
        String suggestedStartCommand,
        String outputDirectory,
        String dockerfilePath
) {}
