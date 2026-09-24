package com.devpilot.ai.cicd.dto;

public record PipelineTriggerRequest(
        String branch,
        String commitSha,
        String commitMessage
) {}
