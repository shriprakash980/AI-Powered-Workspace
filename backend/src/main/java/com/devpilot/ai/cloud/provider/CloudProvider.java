package com.devpilot.ai.cloud.provider;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.cloud.entity.CloudCredential;
import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.model.HealthStatus;

import java.util.Map;

public interface CloudProvider {
    CloudProviderType getProviderType();
    boolean isAvailable();
    boolean validateCredentials(CloudCredential credential);
    DeploymentResult deploy(DeploymentTarget target, ArtifactDto artifact, Map<String, String> envVars);
    void destroy(DeploymentTarget target);
    HealthStatus checkHealth(DeploymentTarget target);

    record DeploymentResult(boolean success, String deploymentUrl, String externalId, String message, String details) {}
}
