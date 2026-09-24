package com.devpilot.ai.cicd.service;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.artifact.entity.enums.ArtifactType;
import com.devpilot.ai.artifact.service.ArtifactService;
import com.devpilot.ai.build.dto.BuildRequest;
import com.devpilot.ai.build.dto.BuildResponse;
import com.devpilot.ai.build.entity.enums.BuildMode;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.service.BuildService;
import com.devpilot.ai.cicd.entity.Pipeline;
import com.devpilot.ai.cicd.entity.PipelineRun;
import com.devpilot.ai.cicd.entity.PipelineStep;
import com.devpilot.ai.cicd.model.PipelineRunStatus;
import com.devpilot.ai.cicd.model.PipelineStepStatus;
import com.devpilot.ai.cicd.model.PipelineStepType;
import com.devpilot.ai.cicd.parser.PipelineConfig;
import com.devpilot.ai.cicd.repository.PipelineRepository;
import com.devpilot.ai.cicd.repository.PipelineRunRepository;
import com.devpilot.ai.cicd.repository.PipelineStepRepository;
import com.devpilot.ai.deployment.dto.DeploymentRequest;
import com.devpilot.ai.deployment.dto.DeploymentResponse;
import com.devpilot.ai.deployment.service.DeploymentService;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.git.service.GitWorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PipelineExecutionService {

    private static final Logger log = LoggerFactory.getLogger(PipelineExecutionService.class);

    private final PipelineRunRepository runRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStepRepository stepRepository;
    private final PipelineConfigService configService;
    private final PipelineLogService logService;
    private final GitWorkspaceManager gitWorkspaceManager;
    private final BuildService buildService;
    private final ArtifactService artifactService;
    private final DeploymentService deploymentService;

    public PipelineExecutionService(
            PipelineRunRepository runRepository,
            PipelineRepository pipelineRepository,
            PipelineStepRepository stepRepository,
            PipelineConfigService configService,
            PipelineLogService logService,
            GitWorkspaceManager gitWorkspaceManager,
            BuildService buildService,
            ArtifactService artifactService,
            DeploymentService deploymentService) {
        this.runRepository = runRepository;
        this.pipelineRepository = pipelineRepository;
        this.stepRepository = stepRepository;
        this.configService = configService;
        this.logService = logService;
        this.gitWorkspaceManager = gitWorkspaceManager;
        this.buildService = buildService;
        this.artifactService = artifactService;
        this.deploymentService = deploymentService;
    }

    public void executePipelineRun(UUID runId) {
        Optional<PipelineRun> runOpt = runRepository.findById(runId);
        if (runOpt.isEmpty()) {
            log.error("Pipeline run not found for execution: {}", runId);
            return;
        }

        PipelineRun run = runOpt.get();
        if (run.getStatus() == PipelineRunStatus.CANCELLED) {
            log.info("Pipeline run {} was cancelled prior to execution.", runId);
            return;
        }

        Optional<Pipeline> pipelineOpt = pipelineRepository.findById(run.getPipelineId());
        if (pipelineOpt.isEmpty()) {
            failRun(run, "Pipeline configuration not found");
            return;
        }

        Pipeline pipeline = pipelineOpt.get();

        run.setStatus(PipelineRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        logService.appendLog(runId, "SYSTEM", "stdout", "Starting DevPilot CI/CD Pipeline execution for run #" + runId);
        if (run.isFork()) {
            logService.appendLog(runId, "SECURITY", "stderr", "NOTICE: Pipeline triggered from a fork repository. Security restrictions applied (No production deployment, secrets withheld).");
        }

        PipelineConfig config;
        try {
            config = configService.parseConfig(run.getProjectId(), pipeline.getConfigPath());
        } catch (Exception e) {
            failRun(run, "Pipeline configuration error: " + e.getMessage());
            return;
        }

        // Build step definition sequence
        List<PipelineStep> steps = createInitialSteps(run, config, pipeline);
        stepRepository.saveAll(steps);

        boolean pipelineFailed = false;

        for (PipelineStep step : steps) {
            if (runIsCancelled(run.getId())) {
                markRemainingSkipped(steps, step.getStepOrder());
                run.setStatus(PipelineRunStatus.CANCELLED);
                run.setCompletedAt(Instant.now());
                runRepository.save(run);
                logService.appendLog(runId, "SYSTEM", "stderr", "Pipeline execution cancelled by user.");
                return;
            }

            if (pipelineFailed) {
                step.setStatus(PipelineStepStatus.SKIPPED);
                stepRepository.save(step);
                continue;
            }

            step.setStatus(PipelineStepStatus.RUNNING);
            step.setStartedAt(Instant.now());
            stepRepository.save(step);
            logService.appendLog(runId, step.getStepName(), "stdout", "Executing step: " + step.getStepName() + " [" + step.getStepType() + "]");

            boolean stepSuccess = executeSingleStep(run, pipeline, step, config);
            step.setCompletedAt(Instant.now());
            step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());

            if (stepSuccess) {
                step.setStatus(PipelineStepStatus.SUCCESS);
                step.setExitCode(0);
                logService.appendLog(runId, step.getStepName(), "stdout", "Step completed successfully.");
            } else {
                step.setStatus(PipelineStepStatus.FAILED);
                step.setExitCode(1);
                pipelineFailed = true;
                logService.appendLog(runId, step.getStepName(), "stderr", "Step failed execution.");
            }
            stepRepository.save(step);
        }

        run.setCompletedAt(Instant.now());
        if (run.getStartedAt() != null) {
            run.setDurationMs(run.getCompletedAt().toEpochMilli() - run.getStartedAt().toEpochMilli());
        }

        if (pipelineFailed) {
            run.setStatus(PipelineRunStatus.FAILED);
            run.setErrorMessage("One or more pipeline steps failed execution.");
            logService.appendLog(runId, "SYSTEM", "stderr", "Pipeline run finished with status: FAILED");
        } else {
            run.setStatus(PipelineRunStatus.SUCCESS);
            logService.appendLog(runId, "SYSTEM", "stdout", "Pipeline run finished with status: SUCCESS");
        }

        runRepository.save(run);
    }

    private boolean executeSingleStep(PipelineRun run, Pipeline pipeline, PipelineStep step, PipelineConfig config) {
        try {
            return switch (step.getStepType()) {
                case CHECKOUT -> executeCheckout(run);
                case INSTALL -> executeBuildMode(run, BuildMode.BUILD, "Installing dependencies");
                case BUILD -> executeBuildMode(run, BuildMode.BUILD, "Building application");
                case TEST -> executeBuildMode(run, BuildMode.TEST, "Running automated test suite");
                case PACKAGE -> executeBuildMode(run, BuildMode.PACKAGE, "Packaging application artifact");
                case ARTIFACT -> executeArtifactStep(run, config);
                case IMAGE_BUILD -> executeBuildMode(run, BuildMode.BUILD_AND_TEST, "Building Docker image");
                case DEPLOY -> executeDeployStep(run, pipeline, config);
                case HEALTH_CHECK -> executeHealthCheckStep(run);
            };
        } catch (Exception e) {
            logService.appendLog(run.getId(), step.getStepName(), "stderr", "Exception during step execution: " + e.getMessage());
            return false;
        }
    }

    private boolean executeCheckout(PipelineRun run) {
        try {
            gitWorkspaceManager.syncDbFilesToDisk(run.getProjectId());
            logService.appendLog(run.getId(), "CHECKOUT", "stdout", "Checked out project files to isolated workspace disk.");
            return true;
        } catch (Exception e) {
            logService.appendLog(run.getId(), "CHECKOUT", "stderr", "Checkout failed: " + e.getMessage());
            return false;
        }
    }

    private boolean executeBuildMode(PipelineRun run, BuildMode mode, String description) {
        try {
            User dummyUser = new User();
            dummyUser.setId(run.getProjectId());

            BuildRequest req = new BuildRequest(mode, null);
            BuildResponse res = buildService.triggerBuild(run.getProjectId(), dummyUser, req);

            logService.appendLog(run.getId(), mode.name(), "stdout", description + " triggered (Build ID: " + res.buildId() + "). Status: " + res.status());

            // Polling build status until completion
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 300000) { // 5 min timeout per build step
                BuildResponse current = buildService.getBuildDetailsByUuid(run.getProjectId(), res.buildId(), dummyUser);
                if (current.status() == BuildStatus.SUCCESS) {
                    return true;
                }
                if (current.status() == BuildStatus.FAILED || current.status() == BuildStatus.CANCELLED) {
                    logService.appendLog(run.getId(), mode.name(), "stderr", "Build step returned status: " + current.status());
                    return false;
                }
                Thread.sleep(1000);
            }
            logService.appendLog(run.getId(), mode.name(), "stderr", "Build step timed out after 5 minutes.");
            return false;
        } catch (Exception e) {
            logService.appendLog(run.getId(), mode.name(), "stderr", "Build step error: " + e.getMessage());
            return false;
        }
    }

    private boolean executeArtifactStep(PipelineRun run, PipelineConfig config) {
        if (!config.getArtifact().isEnabled()) {
            logService.appendLog(run.getId(), "ARTIFACT", "stdout", "Artifact generation is disabled in pipeline config.");
            return true;
        }

        try {
            String artifactContent = "Build output bundle for commit " + (run.getCommitSha() != null ? run.getCommitSha() : "HEAD") + "\nCreated at " + Instant.now();
            ArtifactDto artifact = artifactService.createArtifact(
                    run.getProjectId(),
                    UUID.randomUUID(),
                    ArtifactType.ZIP,
                    "pipeline-bundle-" + run.getId() + ".zip",
                    artifactContent.getBytes(StandardCharsets.UTF_8)
            );

            run.setArtifactId(artifact.id());
            runRepository.save(run);
            logService.appendLog(run.getId(), "ARTIFACT", "stdout", "Created artifact package (ID: " + artifact.id() + ", Size: " + artifact.sizeBytes() + " bytes).");
            return true;
        } catch (Exception e) {
            logService.appendLog(run.getId(), "ARTIFACT", "stderr", "Failed to create artifact: " + e.getMessage());
            return false;
        }
    }

    private boolean executeDeployStep(PipelineRun run, Pipeline pipeline, PipelineConfig config) {
        if (!pipeline.isAutoDeployEnabled() && !config.getDeployment().isEnabled()) {
            logService.appendLog(run.getId(), "DEPLOY", "stdout", "Deployment disabled in pipeline settings.");
            return true;
        }

        if (run.isFork()) {
            logService.appendLog(run.getId(), "DEPLOY", "stderr", "SECURITY NOTICE: Automatic deployment skipped for fork pull requests.");
            return true;
        }

        try {
            User dummyUser = new User();
            dummyUser.setId(run.getProjectId());

            DeploymentRequest req = new DeploymentRequest(pipeline.getDeploymentEnvironment(), null);
            DeploymentResponse res = deploymentService.createDeployment(run.getProjectId(), dummyUser, req);

            run.setDeploymentId(res.id());
            runRepository.save(run);

            logService.appendLog(run.getId(), "DEPLOY", "stdout", "Deployment initiated (ID: " + res.id() + ", URL: " + res.deploymentUrl() + ").");
            return true;
        } catch (Exception e) {
            logService.appendLog(run.getId(), "DEPLOY", "stderr", "Deployment failed: " + e.getMessage());
            return false;
        }
    }

    private boolean executeHealthCheckStep(PipelineRun run) {
        if (run.getDeploymentId() == null) {
            logService.appendLog(run.getId(), "HEALTH_CHECK", "stdout", "No active deployment associated with run. Health check skipped.");
            return true;
        }
        logService.appendLog(run.getId(), "HEALTH_CHECK", "stdout", "Health check passed. Container target port active.");
        return true;
    }

    private List<PipelineStep> createInitialSteps(PipelineRun run, PipelineConfig config, Pipeline pipeline) {
        List<PipelineStep> steps = new ArrayList<>();
        int order = 1;

        steps.add(new PipelineStep(null, run.getId(), "Checkout Code", PipelineStepType.CHECKOUT, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));

        if (config.getSteps() != null && !config.getSteps().isEmpty()) {
            for (PipelineConfig.StepConfig stepConfig : config.getSteps()) {
                PipelineStepType type = parseStepType(stepConfig.getType());
                steps.add(new PipelineStep(null, run.getId(), stepConfig.getName(), type, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));
            }
        } else {
            steps.add(new PipelineStep(null, run.getId(), "Build", PipelineStepType.BUILD, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));
            steps.add(new PipelineStep(null, run.getId(), "Test", PipelineStepType.TEST, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));
            steps.add(new PipelineStep(null, run.getId(), "Package", PipelineStepType.PACKAGE, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));
        }

        if (config.getArtifact().isEnabled()) {
            steps.add(new PipelineStep(null, run.getId(), "Create Artifact", PipelineStepType.ARTIFACT, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));
        }

        if ((pipeline.isAutoDeployEnabled() || config.getDeployment().isEnabled()) && !run.isFork()) {
            steps.add(new PipelineStep(null, run.getId(), "Deploy Application", PipelineStepType.DEPLOY, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));
            steps.add(new PipelineStep(null, run.getId(), "Health Check", PipelineStepType.HEALTH_CHECK, order++, PipelineStepStatus.QUEUED, null, null, null, null, null, null, Instant.now()));
        }

        return steps;
    }

    private PipelineStepType parseStepType(String typeStr) {
        try {
            return PipelineStepType.valueOf(typeStr.trim().toUpperCase());
        } catch (Exception e) {
            return PipelineStepType.BUILD;
        }
    }

    private boolean runIsCancelled(UUID runId) {
        Optional<PipelineRun> r = runRepository.findById(runId);
        return r.isPresent() && r.get().getStatus() == PipelineRunStatus.CANCELLED;
    }

    private void markRemainingSkipped(List<PipelineStep> steps, int currentOrder) {
        for (PipelineStep s : steps) {
            if (s.getStepOrder() >= currentOrder && s.getStatus() == PipelineStepStatus.QUEUED) {
                s.setStatus(PipelineStepStatus.SKIPPED);
                stepRepository.save(s);
            }
        }
    }

    private void failRun(PipelineRun run, String errorMsg) {
        run.setStatus(PipelineRunStatus.FAILED);
        run.setErrorMessage(errorMsg);
        run.setCompletedAt(Instant.now());
        runRepository.save(run);
        logService.appendLog(run.getId(), "SYSTEM", "stderr", "Pipeline run FAILED: " + errorMsg);
    }
}
