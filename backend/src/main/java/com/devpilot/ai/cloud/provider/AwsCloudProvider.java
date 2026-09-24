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
public class AwsCloudProvider implements CloudProvider {

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.AWS;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean validateCredentials(CloudCredential credential) {
        if (credential == null || credential.getAccessKey() == null || credential.getSecretKey() == null) {
            return false;
        }
        return !credential.getAccessKey().isBlank() && !credential.getSecretKey().isBlank();
    }

    @Override
    public DeploymentResult deploy(DeploymentTarget target, ArtifactDto artifact, Map<String, String> envVars) {
        String ecsTaskId = "aws-ecs-task-" + UUID.randomUUID().toString().substring(0, 8);
        String region = target.getRegion() != null ? target.getRegion() : "us-east-1";
        String url = target.getCustomDomain() != null && !target.getCustomDomain().isBlank()
                ? target.getCustomDomain()
                : "https://app-" + target.getId().toString().substring(0, 8) + "." + region + ".elb.amazonaws.com";

        return new DeploymentResult(
                true,
                url,
                ecsTaskId,
                "Successfully deployed container to AWS ECS Fargate cluster (" + region + ")",
                "ECS Task ARN: arn:aws:ecs:" + region + ":123456789012:task/" + ecsTaskId
        );
    }

    @Override
    public void destroy(DeploymentTarget target) {
        // AWS ECS Task / Service teardown simulation
    }

    @Override
    public HealthStatus checkHealth(DeploymentTarget target) {
        return HealthStatus.HEALTHY;
    }
}
