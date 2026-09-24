package com.devpilot.ai.deployment.service;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.artifact.service.ArtifactService;
import com.devpilot.ai.build.dto.BuildResponse;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.repository.BuildRepository;
import com.devpilot.ai.build.service.BuildService;
import com.devpilot.ai.deployment.dto.DeploymentRequest;
import com.devpilot.ai.deployment.dto.DeploymentResponse;
import com.devpilot.ai.deployment.entity.Deployment;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;
import com.devpilot.ai.deployment.provider.DeploymentProvider;
import com.devpilot.ai.deployment.repository.DeploymentRepository;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeploymentService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

    private final DeploymentRepository deploymentRepository;
    private final DeploymentProvider deploymentProvider;
    private final EnvironmentVariableService environmentVariableService;
    private final ArtifactService artifactService;
    private final BuildRepository buildRepository;
    private final BuildService buildService;
    private final ProjectService projectService;

    private static final int MAX_RUNNING_DEPLOYMENTS_PER_USER = 2;

    public DeploymentService(DeploymentRepository deploymentRepository, DeploymentProvider deploymentProvider, EnvironmentVariableService environmentVariableService, ArtifactService artifactService, BuildRepository buildRepository, BuildService buildService, ProjectService projectService) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentProvider = deploymentProvider;
        this.environmentVariableService = environmentVariableService;
        this.artifactService = artifactService;
        this.buildRepository = buildRepository;
        this.buildService = buildService;
        this.projectService = projectService;
    }

    @Transactional
    public DeploymentResponse createDeployment(UUID projectId, User user, DeploymentRequest request) {
        projectService.verifyProjectOwnership(projectId, user.getId());

        long runningCount = deploymentRepository.countByUserIdAndStatus(user.getId(), DeploymentStatus.RUNNING);
        if (runningCount >= MAX_RUNNING_DEPLOYMENTS_PER_USER) {
            throw new IllegalStateException("Maximum running deployments limit (" + MAX_RUNNING_DEPLOYMENTS_PER_USER + ") reached for user.");
        }

        UUID buildId = request.buildId();
        if (buildId == null) {
            var latestBuildOpt = buildRepository.findFirstByProjectIdAndStatusOrderByCreatedAtDesc(projectId, BuildStatus.SUCCESS);
            if (latestBuildOpt.isEmpty()) {
                BuildResponse buildResp = buildService.triggerBuild(projectId, user, new com.devpilot.ai.build.dto.BuildRequest(com.devpilot.ai.build.entity.enums.BuildMode.BUILD, null));
                buildId = buildResp.buildId();
            } else {
                buildId = latestBuildOpt.get().getId();
            }
        }

        DeploymentEnvironment env = request.environment() != null ? request.environment() : DeploymentEnvironment.DEVELOPMENT;

        Deployment deployment = Deployment.builder()
                .projectId(projectId)
                .userId(user.getId())
                .buildId(buildId)
                .environment(env)
                .status(DeploymentStatus.QUEUED)
                .build();

        Deployment saved = deploymentRepository.save(deployment);
        log.info("Queued deployment ID: {} for project: {}", saved.getId(), projectId);

        ArtifactDto artifact = resolveArtifactForBuild(projectId, buildId);
        executeDeploymentAsync(saved, artifact, env);

        return mapToResponse(saved);
    }

    @Async
    public void executeDeploymentAsync(Deployment deployment, ArtifactDto artifact, DeploymentEnvironment env) {
        try {
            Map<String, String> envVars = environmentVariableService.getDecryptedVariablesMap(deployment.getProjectId(), env);
            deploymentProvider.deploy(deployment, artifact, envVars);
        } catch (Exception e) {
            log.error("Deployment execution error for deployment ID: {}", deployment.getId(), e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setStoppedAt(Instant.now());
            deploymentRepository.save(deployment);
        }
    }

    @Transactional(readOnly = true)
    public Page<DeploymentResponse> getProjectDeployments(UUID projectId, User user, int page, int size) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        return deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public DeploymentResponse getDeploymentDetails(UUID projectId, UUID deploymentId, User user) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        Deployment dep = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));
        return mapToResponse(dep);
    }

    @Transactional
    public DeploymentResponse stopDeployment(UUID projectId, UUID deploymentId, User user) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        Deployment dep = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));

        deploymentProvider.stop(dep);
        dep.setStatus(DeploymentStatus.STOPPED);
        dep.setStoppedAt(Instant.now());
        Deployment saved = deploymentRepository.save(dep);

        log.info("Stopped deployment ID: {}", deploymentId);
        return mapToResponse(saved);
    }

    @Transactional
    public DeploymentResponse restartDeployment(UUID projectId, UUID deploymentId, User user) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        Deployment dep = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));

        ArtifactDto artifact = resolveArtifactForBuild(projectId, dep.getBuildId());
        Map<String, String> envVars = environmentVariableService.getDecryptedVariablesMap(projectId, dep.getEnvironment());
        deploymentProvider.restart(dep, artifact, envVars);

        dep.setStatus(DeploymentStatus.RUNNING);
        Deployment saved = deploymentRepository.save(dep);

        log.info("Restarted deployment ID: {}", deploymentId);
        return mapToResponse(saved);
    }

    @Transactional
    public DeploymentResponse rollbackDeployment(UUID projectId, UUID deploymentId, User user) {
        projectService.verifyProjectOwnership(projectId, user.getId());

        List<Deployment> successfulDeployments = deploymentRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, DeploymentStatus.RUNNING);
        Optional<Deployment> priorOpt = successfulDeployments.stream()
                .filter(d -> !d.getId().equals(deploymentId))
                .findFirst();

        if (priorOpt.isEmpty()) {
            throw new IllegalStateException("No prior stable deployment available for rollback.");
        }

        Deployment targetDeployment = priorOpt.get();
        try {
            Deployment current = deploymentRepository.findById(deploymentId).orElse(null);
            if (current != null) {
                deploymentProvider.stop(current);
                current.setStatus(DeploymentStatus.STOPPED);
                deploymentRepository.save(current);
            }
        } catch (Exception e) {
            log.warn("Warning stopping current deployment during rollback: {}", e.getMessage());
        }

        ArtifactDto artifact = resolveArtifactForBuild(projectId, targetDeployment.getBuildId());
        Map<String, String> envVars = environmentVariableService.getDecryptedVariablesMap(projectId, targetDeployment.getEnvironment());
        deploymentProvider.deploy(targetDeployment, artifact, envVars);

        log.info("Rolled back project ID {} to deployment ID {}", projectId, targetDeployment.getId());
        return mapToResponse(targetDeployment);
    }

    private ArtifactDto resolveArtifactForBuild(UUID projectId, UUID buildId) {
        if (buildId == null) return null;
        List<ArtifactDto> artifacts = artifactService.getProjectArtifacts(projectId);
        return artifacts.stream()
                .filter(a -> buildId.equals(a.buildId()))
                .findFirst()
                .orElse(null);
    }

    public DeploymentResponse mapToResponse(Deployment d) {
        return new DeploymentResponse(
                d.getId(),
                d.getProjectId(),
                d.getUserId(),
                d.getBuildId(),
                d.getArtifactId(),
                d.getEnvironment(),
                d.getStatus(),
                d.getDeploymentUrl(),
                d.getRuntime(),
                d.getVersion(),
                d.getAllocatedPort(),
                d.getContainerId(),
                d.getCreatedAt(),
                d.getStoppedAt(),
                d.getCreatedAt()
        );
    }
}
