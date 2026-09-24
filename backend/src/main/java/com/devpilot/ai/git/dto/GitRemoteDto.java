package com.devpilot.ai.git.dto;

public record GitRemoteDto(
        String name,
        String fetchUrl,
        String pushUrl
) {}
