package com.devpilot.ai.git.dto;

public record AiCommitMessageResponse(
        String suggestedMessage,
        String summary
) {}
