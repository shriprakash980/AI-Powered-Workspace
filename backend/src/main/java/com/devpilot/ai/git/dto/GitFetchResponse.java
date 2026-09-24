package com.devpilot.ai.git.dto;

import java.time.Instant;
import java.util.List;

public record GitFetchResponse(
        String remote,
        List<String> updatedRefs,
        Instant timestamp,
        String message
) {}
