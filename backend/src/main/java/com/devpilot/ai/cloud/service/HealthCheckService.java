package com.devpilot.ai.cloud.service;

import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.entity.ServiceHealthCheck;
import com.devpilot.ai.cloud.model.HealthStatus;
import com.devpilot.ai.cloud.provider.CloudProvider;
import com.devpilot.ai.cloud.repository.DeploymentTargetRepository;
import com.devpilot.ai.cloud.repository.ServiceHealthCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final DeploymentTargetRepository deploymentTargetRepository;
    private final ServiceHealthCheckRepository healthCheckRepository;
    private final CloudProviderFactory providerFactory;

    public HealthCheckService(DeploymentTargetRepository deploymentTargetRepository,
                              ServiceHealthCheckRepository healthCheckRepository,
                              CloudProviderFactory providerFactory) {
        this.deploymentTargetRepository = deploymentTargetRepository;
        this.healthCheckRepository = healthCheckRepository;
        this.providerFactory = providerFactory;
    }

    @Transactional
    public ServiceHealthCheck performHealthCheck(DeploymentTarget target) {
        long startTime = System.currentTimeMillis();
        HealthStatus status = HealthStatus.UNKNOWN;
        String details = "Health check initialized";

        try {
            CloudProvider provider = providerFactory.getProvider(target.getProvider());
            status = provider.checkHealth(target);
            details = "Check completed successfully with status: " + status;
        } catch (Exception e) {
            log.error("Error executing health check for target ID {}", target.getId(), e);
            status = HealthStatus.UNHEALTHY;
            details = "Check failed: " + e.getMessage();
        }

        long responseTime = System.currentTimeMillis() - startTime;

        ServiceHealthCheck check = new ServiceHealthCheck();
        check.setDeploymentId(target.getId());
        check.setProjectId(target.getProjectId());
        check.setStatus(status);
        check.setResponseTimeMs(responseTime);
        check.setHealthCheckUrl(target.getCustomDomain() != null ? target.getCustomDomain() : "N/A");
        check.setDetails(details);
        check.setCheckedAt(Instant.now());

        ServiceHealthCheck savedCheck = healthCheckRepository.save(check);

        target.setLastHealthCheckAt(Instant.now());
        deploymentTargetRepository.save(target);

        return savedCheck;
    }

    public List<ServiceHealthCheck> getHealthCheckHistory(UUID projectId) {
        return healthCheckRepository.findByProjectIdOrderByCheckedAtDesc(projectId);
    }

    public Optional<ServiceHealthCheck> getLatestHealthCheck(UUID deploymentId) {
        return healthCheckRepository.findFirstByDeploymentIdOrderByCheckedAtDesc(deploymentId);
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledHealthChecks() {
        log.debug("Running scheduled cloud service health checks...");
        List<DeploymentTarget> activeTargets = deploymentTargetRepository.findAll();
        for (DeploymentTarget target : activeTargets) {
            try {
                performHealthCheck(target);
            } catch (Exception e) {
                log.warn("Scheduled health check failed for target {}", target.getId(), e);
            }
        }
    }
}
