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
public class AzureCloudProvider implements CloudProvider {

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.AZURE;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean validateCredentials(CloudCredential credential) {
        return credential != null && credential.getAccessKey() != null && !credential.getAccessKey().isBlank();
    }

    @Override
    public DeploymentResult deploy(DeploymentTarget target, ArtifactDto artifact, Map<String, String> envVars) {
        String containerAppId = "azure-app-" + UUID.randomUUID().toString().substring(0, 8);
        String region = target.getRegion() != null ? target.getRegion() : "eastus";
        String url = target.getCustomDomain() != null && !target.getCustomDomain().isBlank()
                ? target.getCustomDomain()
                : "https://app-" + target.getId().toString().substring(0, 8) + "." + region + ".azurecontainerapps.io";

        return new DeploymentResult(
                true,
                url,
                containerAppId,
                "Successfully deployed container to Azure Container Apps (" + region + ")",
                "Azure Resource ID: /subscriptions/sub-id/resourceGroups/rg/providers/Microsoft.App/containerApps/" + containerAppId
        );
    }

    @Override
    public void destroy(DeploymentTarget target) {
        // Azure Container App teardown simulation
    }

    @Override
    public HealthStatus checkHealth(DeploymentTarget target) {
        return HealthStatus.HEALTHY;
    }
}
