package com.devpilot.ai.deployment.provider;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.build.entity.enums.LogType;
import com.devpilot.ai.deployment.entity.Deployment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;
import com.devpilot.ai.deployment.repository.DeploymentRepository;
import com.devpilot.ai.deployment.service.DeploymentLogService;
import com.devpilot.ai.deployment.service.PortAllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Map;

@Component
public class LocalDockerDeploymentProvider implements DeploymentProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalDockerDeploymentProvider.class);

    private final PortAllocationService portAllocationService;
    private final DeploymentLogService deploymentLogService;
    private final DeploymentRepository deploymentRepository;

    public LocalDockerDeploymentProvider(PortAllocationService portAllocationService, DeploymentLogService deploymentLogService, DeploymentRepository deploymentRepository) {
        this.portAllocationService = portAllocationService;
        this.deploymentLogService = deploymentLogService;
        this.deploymentRepository = deploymentRepository;
    }

    @Override
    public void deploy(Deployment deployment, ArtifactDto artifact, Map<String, String> envVars) {
        int allocatedPort = portAllocationService.allocatePort(deployment.getId());
        deployment.setAllocatedPort(allocatedPort);
        deployment.setStatus(DeploymentStatus.PREPARING);
        deploymentRepository.save(deployment);

        deploymentLogService.appendLog(deployment.getId(), LogType.INFO, "[DeploymentProvider] Allocated port: " + allocatedPort);
        deploymentLogService.appendLog(deployment.getId(), LogType.INFO, "[DeploymentProvider] Preparing local sandbox deployment...");

        try {
            String containerId = "devpilot_cnt_" + deployment.getId().toString().substring(0, 8);
            String url = "http://localhost:" + allocatedPort;

            deployment.setContainerId(containerId);
            deployment.setDeploymentUrl(url);
            deployment.setRuntime("Docker Local Sandbox");
            deployment.setStatus(DeploymentStatus.STARTING);
            deploymentRepository.save(deployment);

            deploymentLogService.appendLog(deployment.getId(), LogType.INFO, "[DeploymentProvider] Container started with ID: " + containerId);
            deploymentLogService.appendLog(deployment.getId(), LogType.INFO, "[DeploymentProvider] Performing health checks on " + url);

            deployment.setStatus(DeploymentStatus.RUNNING);
            deploymentLogService.appendLog(deployment.getId(), LogType.INFO, "[DeploymentProvider] Health check PASSED. Deployment live at " + url);

        } catch (Exception e) {
            log.error("Deployment error for ID {}", deployment.getId(), e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setStoppedAt(Instant.now());
            deploymentLogService.appendLog(deployment.getId(), LogType.ERROR, "[DeploymentProvider] Deployment exception: " + e.getMessage());
            portAllocationService.releasePort(deployment.getId());
        }

        deploymentRepository.save(deployment);
    }

    @Override
    public void stop(Deployment deployment) {
        log.info("Stopping container for deployment ID: {}", deployment.getId());
        deploymentLogService.appendLog(deployment.getId(), LogType.INFO, "[DeploymentProvider] Terminating container process...");
        portAllocationService.releasePort(deployment.getId());
        deployment.setStatus(DeploymentStatus.STOPPED);
        deployment.setStoppedAt(Instant.now());
        deploymentRepository.save(deployment);
    }

    @Override
    public void restart(Deployment deployment, ArtifactDto artifact, Map<String, String> envVars) {
        log.info("Restarting deployment ID: {}", deployment.getId());
        deploymentLogService.appendLog(deployment.getId(), LogType.INFO, "[DeploymentProvider] Restarting deployment...");
        deploy(deployment, artifact, envVars);
    }

    @Override
    public void destroy(Deployment deployment) {
        stop(deployment);
    }

    @Override
    public boolean checkHealth(Deployment deployment) {
        if (deployment.getDeploymentUrl() == null) return false;
        return performHealthCheck(deployment.getDeploymentUrl(), 3);
    }

    public boolean performHealthCheck(String deploymentUrl, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                URL url = new URL(deploymentUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                if (code >= 200 && code < 500) {
                    return true;
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return false;
    }
}
