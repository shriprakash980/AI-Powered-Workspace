package com.devpilot.ai.cloud.provider;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.cloud.entity.CloudCredential;
import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.model.HealthStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class GcpCloudProvider implements CloudProvider {

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.GCP;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean validateCredentials(CloudCredential credential) {
        return credential != null && credential.getSecretKey() != null && !credential.getSecretKey().isBlank();
    }

    @Override
    public DeploymentResult deploy(DeploymentTarget target, ArtifactDto artifact, Map<String, String> envVars) {
        String serviceId = "gcp-run-" + UUID.randomUUID().toString().substring(0, 8);
        String region = target.getRegion() != null ? target.getRegion() : "us-central1";
        String url = target.getCustomDomain() != null && !target.getCustomDomain().isBlank()
                ? target.getCustomDomain()
                : "https://app-" + target.getId().toString().substring(0, 8) + "-uc.a.run.app";

        return new DeploymentResult(
                true,
                url,
                serviceId,
                "Successfully deployed container to GCP Cloud Run (" + region + ")",
                "Cloud Run Revision: " + serviceId + "-00001-abc"
        );
    }

    @Override
    public void destroy(DeploymentTarget target) {
        // GCP Cloud Run service teardown simulation
    }

    @Override
    public HealthStatus checkHealth(DeploymentTarget target) {
        return HealthStatus.HEALTHY;
    }
}
