package com.devpilot.ai.cloud.service;

import com.devpilot.ai.cloud.dto.*;
import com.devpilot.ai.cloud.entity.CloudCredential;
import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.model.CloudTargetStatus;
import com.devpilot.ai.cloud.repository.CloudCredentialRepository;
import com.devpilot.ai.cloud.repository.DeploymentTargetRepository;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CloudDeploymentTargetService {

    private final DeploymentTargetRepository deploymentTargetRepository;
    private final CloudCredentialRepository cloudCredentialRepository;
    private final SecretManager secretManager;

    public CloudDeploymentTargetService(DeploymentTargetRepository deploymentTargetRepository,
                                        CloudCredentialRepository cloudCredentialRepository,
                                        SecretManager secretManager) {
        this.deploymentTargetRepository = deploymentTargetRepository;
        this.cloudCredentialRepository = cloudCredentialRepository;
        this.secretManager = secretManager;
    }

    @Transactional
    public CloudCredentialResponse createCredential(UUID userId, CloudCredentialRequest request) {
        String encryptedAccessKey = secretManager.encrypt(request.accessKey());
        String encryptedSecretKey = secretManager.encrypt(request.secretKey());

        CloudCredential credential = new CloudCredential();
        credential.setUserId(userId);
        credential.setName(request.name());
        credential.setProvider(request.provider());
        credential.setAccessKey(encryptedAccessKey);
        credential.setSecretKey(encryptedSecretKey);
        credential.setRegion(request.region());
        credential.setMetadataJson(request.metadataJson());
        credential.setCreatedAt(Instant.now());

        CloudCredential saved = cloudCredentialRepository.save(credential);
        return mapCredentialToResponse(saved);
    }

    public List<CloudCredentialResponse> getUserCredentials(UUID userId) {
        return cloudCredentialRepository.findByUserId(userId).stream()
                .map(this::mapCredentialToResponse)
                .toList();
    }

    @Transactional
    public DeploymentTargetResponse createDeploymentTarget(UUID projectId, DeploymentTargetRequest request) {
        DeploymentTarget target = new DeploymentTarget();
        target.setProjectId(projectId);
        target.setName(request.name());
        target.setProvider(request.provider());
        target.setEnvironment(request.environment());
        target.setStatus(CloudTargetStatus.ACTIVE);
        target.setCredentialId(request.credentialId());
        target.setRegion(request.region() != null ? request.region() : "us-east-1");
        target.setCustomDomain(request.customDomain());
        target.setConfigurationJson(request.configurationJson());

        boolean approvalRequired = request.environment() == DeploymentEnvironment.PRODUCTION;
        target.setApprovalRequired(approvalRequired);
        target.setApproved(!approvalRequired);
        target.setCreatedAt(Instant.now());

        DeploymentTarget saved = deploymentTargetRepository.save(target);
        return mapTargetToResponse(saved);
    }

    public List<DeploymentTargetResponse> getProjectTargets(UUID projectId) {
        return deploymentTargetRepository.findByProjectId(projectId).stream()
                .map(this::mapTargetToResponse)
                .toList();
    }

    public DeploymentTarget getTargetEntity(UUID targetId, UUID projectId) {
        return deploymentTargetRepository.findByIdAndProjectId(targetId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment target not found for targetId: " + targetId));
    }

    public DeploymentTargetResponse getTargetResponse(UUID targetId, UUID projectId) {
        return mapTargetToResponse(getTargetEntity(targetId, projectId));
    }

    private CloudCredentialResponse mapCredentialToResponse(CloudCredential credential) {
        return new CloudCredentialResponse(
                credential.getId(),
                credential.getName(),
                credential.getProvider(),
                credential.getRegion(),
                credential.getCreatedAt()
        );
    }

    public DeploymentTargetResponse mapTargetToResponse(DeploymentTarget target) {
        return new DeploymentTargetResponse(
                target.getId(),
                target.getProjectId(),
                target.getName(),
                target.getProvider(),
                target.getEnvironment(),
                target.getStatus(),
                target.getRegion(),
                target.getCustomDomain(),
                target.isApprovalRequired(),
                target.isApproved(),
                target.getLastHealthCheckAt(),
                target.getCreatedAt()
        );
    }
}
