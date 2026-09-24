package com.devpilot.ai.deployment.dto;

import java.util.UUID;

public record DeploymentDiagnosisResponse(
        UUID deploymentId,
        String rootCause,
        String suggestedFix
) {}
