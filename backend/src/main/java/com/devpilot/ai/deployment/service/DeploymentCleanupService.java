package com.devpilot.ai.deployment.service;

import com.devpilot.ai.deployment.entity.Deployment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;
import com.devpilot.ai.deployment.repository.DeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DeploymentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentCleanupService.class);

    private final DeploymentRepository deploymentRepository;
    private final PortAllocationService portAllocationService;

    public DeploymentCleanupService(DeploymentRepository deploymentRepository, PortAllocationService portAllocationService) {
        this.deploymentRepository = deploymentRepository;
        this.portAllocationService = portAllocationService;
    }

    @Scheduled(cron = "0 0 * * * *") // Run hourly
    public void cleanupAbandonedDeployments() {
        log.info("Running scheduled cleanup for abandoned deployments...");
        List<Deployment> running = deploymentRepository.findAll().stream()
                .filter(d -> d.getStatus() == DeploymentStatus.RUNNING)
                .toList();

        Instant dayAgo = Instant.now().minusSeconds(86400);
        for (Deployment d : running) {
            if (d.getCreatedAt() != null && d.getCreatedAt().isBefore(dayAgo)) {
                log.info("Auto-stopping old deployment ID: {}", d.getId());
                portAllocationService.releasePort(d.getId());
                d.setStatus(DeploymentStatus.STOPPED);
                d.setStoppedAt(Instant.now());
                deploymentRepository.save(d);
            }
        }
    }
}
