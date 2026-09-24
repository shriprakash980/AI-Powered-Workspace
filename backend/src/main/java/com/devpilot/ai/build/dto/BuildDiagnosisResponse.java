package com.devpilot.ai.build.dto;

import java.util.UUID;

public record BuildDiagnosisResponse(
        UUID buildId,
        String diagnosis,
        String suggestedFix,
        String relevantFile,
        String proposedPatch
) {
    public BuildDiagnosisResponse(UUID buildId, String diagnosis, String suggestedFix) {
        this(buildId, diagnosis, suggestedFix, null, null);
    }
}
