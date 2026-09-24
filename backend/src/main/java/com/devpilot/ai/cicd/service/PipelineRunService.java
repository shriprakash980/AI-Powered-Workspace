package com.devpilot.ai.cicd.service;

import com.devpilot.ai.cicd.dto.PipelineRunResponse;
import com.devpilot.ai.cicd.dto.PipelineStepResponse;
import com.devpilot.ai.cicd.entity.Pipeline;
import com.devpilot.ai.cicd.entity.PipelineRun;
import com.devpilot.ai.cicd.entity.PipelineStep;
import com.devpilot.ai.cicd.model.PipelineRunStatus;
import com.devpilot.ai.cicd.model.PipelineTriggerType;
import com.devpilot.ai.cicd.repository.PipelineRepository;
import com.devpilot.ai.cicd.repository.PipelineRunRepository;
import com.devpilot.ai.cicd.repository.PipelineStepRepository;
import com.devpilot.ai.cicd.worker.PipelineJob;
import com.devpilot.ai.cicd.worker.PipelineQueue;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PipelineRunService {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunService.class);

    private final PipelineRunRepository runRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStepRepository stepRepository;
    private final ProjectService projectService;
    private final PipelineQueue pipelineQueue;

    public PipelineRunService(
            PipelineRunRepository runRepository,
            PipelineRepository pipelineRepository,
            PipelineStepRepository stepRepository,
            ProjectService projectService,
            PipelineQueue pipelineQueue) {
        this.runRepository = runRepository;
        this.pipelineRepository = pipelineRepository;
        this.stepRepository = stepRepository;
        this.projectService = projectService;
        this.pipelineQueue = pipelineQueue;
    }

    @Transactional(readOnly = true)
    public Page<PipelineRunResponse> getPipelineRuns(UUID projectId, UUID pipelineId, UUID userId, int page, int size) {
        projectService.verifyProjectOwnership(projectId, userId);
        return runRepository.findByPipelineIdOrderByCreatedAtDesc(pipelineId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PipelineRunResponse getRunDetails(UUID projectId, UUID pipelineId, UUID runId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        PipelineRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline run not found: " + runId));
        return mapToResponse(run);
    }

    @Transactional(readOnly = true)
    public List<PipelineStepResponse> getRunSteps(UUID projectId, UUID pipelineId, UUID runId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        return stepRepository.findByPipelineRunIdOrderByStepOrderAsc(runId)
                .stream()
                .map(this::mapStepToResponse)
                .toList();
    }

    @Transactional
    public PipelineRunResponse cancelPipelineRun(UUID projectId, UUID pipelineId, UUID runId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        PipelineRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline run not found: " + runId));

        if (run.getStatus() == PipelineRunStatus.QUEUED || run.getStatus() == PipelineRunStatus.RUNNING) {
            run.setStatus(PipelineRunStatus.CANCELLED);
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            log.info("Cancelled pipeline run ID: {}", runId);
        }

        return mapToResponse(run);
    }

    @Transactional
    public PipelineRunResponse rerunPipelineRun(UUID projectId, UUID pipelineId, UUID runId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        PipelineRun originalRun = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline run not found: " + runId));

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + pipelineId));

        PipelineRun newRun = new PipelineRun(
                null,
                pipeline.getId(),
                projectId,
                originalRun.getCommitSha(),
                originalRun.getBranch(),
                "Rerun: " + (originalRun.getCommitMessage() != null ? originalRun.getCommitMessage() : "Manual rerun"),
                PipelineTriggerType.MANUAL,
                userId.toString(),
                PipelineRunStatus.QUEUED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                originalRun.isFork(),
                Instant.now()
        );

        PipelineRun savedRun = runRepository.save(newRun);
        log.info("Queued rerun ID: {} for original run ID: {}", savedRun.getId(), runId);

        pipelineQueue.enqueue(new PipelineJob(savedRun.getId(), projectId, pipelineId, savedRun.isFork()));

        return mapToResponse(savedRun);
    }

    public PipelineRunResponse mapToResponse(PipelineRun run) {
        return new PipelineRunResponse(
                run.getId(),
                run.getPipelineId(),
                run.getProjectId(),
                run.getCommitSha(),
                run.getBranch(),
                run.getCommitMessage(),
                run.getTriggerType(),
                run.getTriggeredBy(),
                run.getStatus(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getDurationMs(),
                run.getErrorMessage(),
                run.getArtifactId(),
                run.getDeploymentId(),
                run.isFork(),
                run.getCreatedAt()
        );
    }

    private PipelineStepResponse mapStepToResponse(PipelineStep step) {
        return new PipelineStepResponse(
                step.getId(),
                step.getPipelineRunId(),
                step.getStepName(),
                step.getStepType(),
                step.getStepOrder(),
                step.getStatus(),
                step.getStartedAt(),
                step.getCompletedAt(),
                step.getDurationMs(),
                step.getExitCode(),
                step.getErrorMessage(),
                step.getCreatedAt()
        );
    }
}
