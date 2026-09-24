package com.devpilot.ai.deployment.provider;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.deployment.entity.Deployment;

import java.util.Map;

public interface DeploymentProvider {
    void deploy(Deployment deployment, ArtifactDto artifact, Map<String, String> envVars);
    void stop(Deployment deployment);
    void restart(Deployment deployment, ArtifactDto artifact, Map<String, String> envVars);
    void destroy(Deployment deployment);
    boolean checkHealth(Deployment deployment);
}
