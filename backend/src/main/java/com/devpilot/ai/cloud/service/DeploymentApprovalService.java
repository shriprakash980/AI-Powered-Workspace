package com.devpilot.ai.cloud.service;

import com.devpilot.ai.cloud.entity.DeploymentEvent;
import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.model.DeploymentEventType;
import com.devpilot.ai.cloud.repository.DeploymentEventRepository;
import com.devpilot.ai.cloud.repository.DeploymentTargetRepository;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeploymentApprovalService {

    private final DeploymentTargetRepository deploymentTargetRepository;
    private final DeploymentEventRepository deploymentEventRepository;

    public DeploymentApprovalService(DeploymentTargetRepository deploymentTargetRepository,
                                     DeploymentEventRepository deploymentEventRepository) {
        this.deploymentTargetRepository = deploymentTargetRepository;
        this.deploymentEventRepository = deploymentEventRepository;
    }

    public boolean requiresApproval(DeploymentEnvironment environment) {
        return environment == DeploymentEnvironment.PRODUCTION;
    }

    @Transactional
    public void requestApproval(DeploymentTarget target, String requestedBy) {
        target.setApprovalRequired(true);
        target.setApproved(false);
        deploymentTargetRepository.save(target);

        DeploymentEvent event = new DeploymentEvent();
        event.setDeploymentId(target.getId());
        event.setProjectId(target.getProjectId());
        event.setEventType(DeploymentEventType.APPROVAL_REQUESTED);
        event.setMessage("Production deployment approval requested by " + requestedBy);
        event.setDetails("Environment: PRODUCTION, Target: " + target.getName());
        event.setCreatedAt(Instant.now());
        deploymentEventRepository.save(event);
    }

    @Transactional
    public void approveDeployment(UUID targetId, String approvedBy, String notes) {
        DeploymentTarget target = deploymentTargetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment target not found: " + targetId));

        target.setApproved(true);
        deploymentTargetRepository.save(target);

        DeploymentEvent event = new DeploymentEvent();
        event.setDeploymentId(target.getId());
        event.setProjectId(target.getProjectId());
        event.setEventType(DeploymentEventType.APPROVED);
        event.setMessage("Deployment approved by " + approvedBy);
        event.setDetails(notes != null ? notes : "Approved for production deployment");
        event.setCreatedAt(Instant.now());
        deploymentEventRepository.save(event);
    }

    @Transactional
    public void rejectDeployment(UUID targetId, String rejectedBy, String reason) {
        DeploymentTarget target = deploymentTargetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment target not found: " + targetId));

        target.setApproved(false);
        deploymentTargetRepository.save(target);

        DeploymentEvent event = new DeploymentEvent();
        event.setDeploymentId(target.getId());
        event.setProjectId(target.getProjectId());
        event.setEventType(DeploymentEventType.REJECTED);
        event.setMessage("Deployment rejected by " + rejectedBy);
        event.setDetails(reason != null ? reason : "Rejected during approval review");
        event.setCreatedAt(Instant.now());
        deploymentEventRepository.save(event);
    }
}
