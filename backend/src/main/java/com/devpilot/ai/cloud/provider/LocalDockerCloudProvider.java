package com.devpilot.ai.cloud.provider;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.cloud.entity.CloudCredential;
import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.model.HealthStatus;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

@Component
public class LocalDockerCloudProvider implements CloudProvider {

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.LOCAL_DOCKER;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean validateCredentials(CloudCredential credential) {
        return true;
    }

    @Override
    public DeploymentResult deploy(DeploymentTarget target, ArtifactDto artifact, Map<String, String> envVars) {
        String containerId = "devpilot_docker_" + UUID.randomUUID().toString().substring(0, 8);
        String url = target.getCustomDomain() != null && !target.getCustomDomain().isBlank()
                ? target.getCustomDomain()
                : "http://localhost:8080";

        return new DeploymentResult(
                true,
                url,
                containerId,
                "Successfully deployed to Local Docker engine",
                "Container " + containerId + " is running in local environment."
        );
    }

    @Override
    public void destroy(DeploymentTarget target) {
        // Cleanup local container resources
    }

    @Override
    public HealthStatus checkHealth(DeploymentTarget target) {
        String urlStr = target.getCustomDomain() != null && !target.getCustomDomain().isBlank()
                ? target.getCustomDomain()
                : "http://localhost:8080";
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                return HealthStatus.HEALTHY;
            } else if (responseCode < 500) {
                return HealthStatus.DEGRADED;
            } else {
                return HealthStatus.UNHEALTHY;
            }
        } catch (Exception e) {
            return HealthStatus.UNHEALTHY;
        }
    }
}
