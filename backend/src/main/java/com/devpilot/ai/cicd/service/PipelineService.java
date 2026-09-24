package com.devpilot.ai.cicd.service;

import com.devpilot.ai.cicd.dto.PipelineCreateRequest;
import com.devpilot.ai.cicd.dto.PipelineResponse;
import com.devpilot.ai.cicd.dto.PipelineRunResponse;
import com.devpilot.ai.cicd.dto.PipelineTriggerRequest;
import com.devpilot.ai.cicd.dto.PipelineUpdateRequest;
import com.devpilot.ai.cicd.entity.Pipeline;
import com.devpilot.ai.cicd.entity.PipelineRun;
import com.devpilot.ai.cicd.model.PipelineTriggerType;
import com.devpilot.ai.cicd.repository.PipelineRepository;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final PipelineRepository pipelineRepository;
    private final ProjectService projectService;
    private final PipelineTriggerService triggerService;
    private final PipelineRunService runService;

    public PipelineService(
            PipelineRepository pipelineRepository,
            ProjectService projectService,
            PipelineTriggerService triggerService,
            PipelineRunService runService) {
        this.pipelineRepository = pipelineRepository;
        this.projectService = projectService;
        this.triggerService = triggerService;
        this.runService = runService;
    }

    @Transactional
    public PipelineResponse createPipeline(UUID projectId, UUID userId, PipelineCreateRequest request) {
        projectService.verifyProjectOwnership(projectId, userId);

        Pipeline pipeline = new Pipeline(
                null,
                projectId,
                request.name(),
                request.description(),
                request.enabled() != null ? request.enabled() : true,
                request.configPath() != null ? request.configPath() : "devpilot-ci.yml",
                request.defaultBranch() != null ? request.defaultBranch() : "main",
                request.triggerOnPush() != null ? request.triggerOnPush() : true,
                request.triggerOnPullRequest() != null ? request.triggerOnPullRequest() : true,
                request.allowManualTrigger() != null ? request.allowManualTrigger() : true,
                request.autoDeployEnabled() != null ? request.autoDeployEnabled() : false,
                request.deploymentEnvironment() != null ? request.deploymentEnvironment() : DeploymentEnvironment.DEVELOPMENT,
                Instant.now(),
                Instant.now()
        );

        Pipeline saved = pipelineRepository.save(pipeline);
        log.info("Created pipeline ID: {} for project ID: {}", saved.getId(), projectId);
        return mapToResponse(saved);
    }

    @Transactional
    public List<PipelineResponse> getProjectPipelines(UUID projectId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        List<Pipeline> pipelines = pipelineRepository.findByProjectId(projectId);

        if (pipelines.isEmpty()) {
            // Create default initial pipeline
            Pipeline defaultPipeline = new Pipeline(
                    null,
                    projectId,
                    "Default CI/CD Pipeline",
                    "Automated build, test, and package pipeline",
                    true,
                    "devpilot-ci.yml",
                    "main",
                    true,
                    true,
                    true,
                    false,
                    DeploymentEnvironment.DEVELOPMENT,
                    Instant.now(),
                    Instant.now()
            );
            Pipeline saved = pipelineRepository.save(defaultPipeline);
            return List.of(mapToResponse(saved));
        }

        return pipelines.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public PipelineResponse getPipelineDetails(UUID projectId, UUID pipelineId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        Pipeline pipeline = pipelineRepository.findByIdAndProjectId(pipelineId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + pipelineId));
        return mapToResponse(pipeline);
    }

    @Transactional
    public PipelineResponse updatePipeline(UUID projectId, UUID pipelineId, UUID userId, PipelineUpdateRequest request) {
        projectService.verifyProjectOwnership(projectId, userId);
        Pipeline pipeline = pipelineRepository.findByIdAndProjectId(pipelineId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + pipelineId));

        if (request.name() != null && !request.name().isBlank()) pipeline.setName(request.name());
        if (request.description() != null) pipeline.setDescription(request.description());
        if (request.enabled() != null) pipeline.setEnabled(request.enabled());
        if (request.configPath() != null) pipeline.setConfigPath(request.configPath());
        if (request.defaultBranch() != null) pipeline.setDefaultBranch(request.defaultBranch());
        if (request.triggerOnPush() != null) pipeline.setTriggerOnPush(request.triggerOnPush());
        if (request.triggerOnPullRequest() != null) pipeline.setTriggerOnPullRequest(request.triggerOnPullRequest());
        if (request.allowManualTrigger() != null) pipeline.setAllowManualTrigger(request.allowManualTrigger());
        if (request.autoDeployEnabled() != null) pipeline.setAutoDeployEnabled(request.autoDeployEnabled());
        if (request.deploymentEnvironment() != null) pipeline.setDeploymentEnvironment(request.deploymentEnvironment());
        pipeline.setUpdatedAt(Instant.now());

        Pipeline updated = pipelineRepository.save(pipeline);
        log.info("Updated pipeline ID: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Transactional
    public void deletePipeline(UUID projectId, UUID pipelineId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        Pipeline pipeline = pipelineRepository.findByIdAndProjectId(pipelineId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + pipelineId));
        pipelineRepository.delete(pipeline);
        log.info("Deleted pipeline ID: {}", pipelineId);
    }

    @Transactional
    public PipelineRunResponse triggerPipelineManual(UUID projectId, UUID pipelineId, UUID userId, PipelineTriggerRequest request) {
        projectService.verifyProjectOwnership(projectId, userId);
        Pipeline pipeline = pipelineRepository.findByIdAndProjectId(pipelineId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + pipelineId));

        if (!pipeline.isEnabled()) {
            throw new IllegalStateException("Pipeline is disabled.");
        }
        if (!pipeline.isAllowManualTrigger()) {
            throw new IllegalStateException("Manual trigger is disabled for this pipeline.");
        }

        Project project = projectService.getProjectEntityByIdAndUser(projectId, userId);
        String branch = (request != null && request.branch() != null && !request.branch().isBlank()) ? request.branch() : pipeline.getDefaultBranch();
        String commitSha = (request != null && request.commitSha() != null && !request.commitSha().isBlank()) ? request.commitSha() : "HEAD";
        String commitMsg = (request != null && request.commitMessage() != null && !request.commitMessage().isBlank()) ? request.commitMessage() : "Manual pipeline trigger";

        PipelineRun run = triggerService.triggerPipelineRun(
                pipeline,
                project,
                commitSha,
                branch,
                commitMsg,
                PipelineTriggerType.MANUAL,
                userId.toString(),
                null,
                false
        );

        return runService.mapToResponse(run);
    }

    public PipelineResponse mapToResponse(Pipeline p) {
        return new PipelineResponse(
                p.getId(),
                p.getProjectId(),
                p.getName(),
                p.getDescription(),
                p.isEnabled(),
                p.getConfigPath(),
                p.getDefaultBranch(),
                p.isTriggerOnPush(),
                p.isTriggerOnPullRequest(),
                p.isAllowManualTrigger(),
                p.isAutoDeployEnabled(),
                p.getDeploymentEnvironment(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
