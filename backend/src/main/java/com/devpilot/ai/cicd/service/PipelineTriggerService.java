package com.devpilot.ai.cicd.service;

import com.devpilot.ai.cicd.entity.Pipeline;
import com.devpilot.ai.cicd.entity.PipelineRun;
import com.devpilot.ai.cicd.model.PipelineRunStatus;
import com.devpilot.ai.cicd.model.PipelineTriggerType;
import com.devpilot.ai.cicd.repository.PipelineRepository;
import com.devpilot.ai.cicd.repository.PipelineRunRepository;
import com.devpilot.ai.cicd.worker.PipelineJob;
import com.devpilot.ai.cicd.worker.PipelineQueue;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PipelineTriggerService {

    private static final Logger log = LoggerFactory.getLogger(PipelineTriggerService.class);

    private final ProjectRepository projectRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final PipelineQueue pipelineQueue;
    private final ObjectMapper objectMapper;

    public PipelineTriggerService(
            ProjectRepository projectRepository,
            PipelineRepository pipelineRepository,
            PipelineRunRepository pipelineRunRepository,
            PipelineQueue pipelineQueue) {
        this.projectRepository = projectRepository;
        this.pipelineRepository = pipelineRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.pipelineQueue = pipelineQueue;
        this.objectMapper = new ObjectMapper();
    }

    public void processGitHubWebhookEvent(String eventType, String deliveryId, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            log.warn("Empty payload received for GitHub webhook event: {}", eventType);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            String repoFullName = extractRepoFullName(root);
            if (repoFullName == null) {
                log.warn("Could not resolve repository name from webhook event payload.");
                return;
            }

            Optional<Project> projectOpt = findProjectByRepoName(repoFullName);
            if (projectOpt.isEmpty()) {
                log.info("No connected DevPilot project found for GitHub repository: {}", repoFullName);
                return;
            }

            Project project = projectOpt.get();
            Optional<Pipeline> pipelineOpt = pipelineRepository.findFirstByProjectIdAndEnabledTrue(project.getId());
            if (pipelineOpt.isEmpty()) {
                log.info("No enabled pipeline found for project ID: {}", project.getId());
                return;
            }

            Pipeline pipeline = pipelineOpt.get();

            if ("push".equalsIgnoreCase(eventType)) {
                handlePushEvent(root, project, pipeline, deliveryId);
            } else if ("pull_request".equalsIgnoreCase(eventType)) {
                handlePullRequestEvent(root, project, pipeline, deliveryId);
            } else {
                log.info("Ignored unsupported GitHub webhook event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process GitHub webhook event (type: {}): {}", eventType, e.getMessage(), e);
        }
    }

    private void handlePushEvent(JsonNode root, Project project, Pipeline pipeline, String deliveryId) {
        if (!pipeline.isTriggerOnPush()) {
            log.info("Push trigger disabled for pipeline ID: {}", pipeline.getId());
            return;
        }

        String ref = root.path("ref").asText(""); // refs/heads/main
        String branch = ref.replace("refs/heads/", "");
        String commitSha = root.path("after").asText("");
        String commitMessage = root.path("head_commit").path("message").asText("GitHub Push Event");
        String author = root.path("head_commit").path("author").path("username").asText("GitHub");

        if (commitSha.isBlank() || commitSha.equals("0000000000000000000000000000000000000000")) {
            log.info("Branch deletion push event ignored.");
            return;
        }

        triggerPipelineRun(pipeline, project, commitSha, branch, commitMessage, PipelineTriggerType.PUSH, author, deliveryId, false);
    }

    private void handlePullRequestEvent(JsonNode root, Project project, Pipeline pipeline, String deliveryId) {
        if (!pipeline.isTriggerOnPullRequest()) {
            log.info("Pull request trigger disabled for pipeline ID: {}", pipeline.getId());
            return;
        }

        String action = root.path("action").asText("");
        if (!"opened".equalsIgnoreCase(action) && !"synchronize".equalsIgnoreCase(action) && !"reopened".equalsIgnoreCase(action)) {
            log.info("Ignored pull request action: {}", action);
            return;
        }

        JsonNode prNode = root.path("pull_request");
        String branch = prNode.path("head").path("ref").asText("main");
        String commitSha = prNode.path("head").path("sha").asText("");
        String commitMessage = "PR #" + root.path("number").asText() + ": " + prNode.path("title").asText();
        String author = prNode.path("user").path("login").asText("GitHub");

        boolean isFork = prNode.path("head").path("repo").path("fork").asBoolean(false);

        triggerPipelineRun(pipeline, project, commitSha, branch, commitMessage, PipelineTriggerType.PULL_REQUEST, author, deliveryId, isFork);
    }

    public PipelineRun triggerPipelineRun(Pipeline pipeline, Project project, String commitSha, String branch, String commitMessage, PipelineTriggerType triggerType, String triggeredBy, String deliveryId, boolean isFork) {
        // Idempotency / duplicate check
        if (deliveryId != null && !deliveryId.isBlank()) {
            Optional<PipelineRun> existingByDelivery = pipelineRunRepository.findByDeliveryId(deliveryId);
            if (existingByDelivery.isPresent()) {
                log.info("Duplicate webhook delivery ID: {}. Returning existing run ID: {}", deliveryId, existingByDelivery.get().getId());
                return existingByDelivery.get();
            }
        }

        Optional<PipelineRun> existingActive = pipelineRunRepository.findFirstByPipelineIdAndCommitShaAndTriggerTypeAndStatus(
                pipeline.getId(), commitSha, triggerType, PipelineRunStatus.QUEUED);
        if (existingActive.isPresent()) {
            log.info("Duplicate trigger attempt for pipeline ID {} and commit {}. Returning active run ID: {}", pipeline.getId(), commitSha, existingActive.get().getId());
            return existingActive.get();
        }

        PipelineRun run = new PipelineRun(
                null,
                pipeline.getId(),
                project.getId(),
                commitSha,
                branch,
                commitMessage,
                triggerType,
                triggeredBy,
                PipelineRunStatus.QUEUED,
                null,
                null,
                null,
                null,
                null,
                null,
                deliveryId,
                isFork,
                Instant.now()
        );

        PipelineRun savedRun = pipelineRunRepository.save(run);
        log.info("Created pipeline run ID: {} (Trigger: {}, Branch: {}, Commit: {})", savedRun.getId(), triggerType, branch, commitSha);

        PipelineJob job = new PipelineJob(savedRun.getId(), project.getId(), pipeline.getId(), isFork);
        pipelineQueue.enqueue(job);

        return savedRun;
    }

    private String extractRepoFullName(JsonNode root) {
        String name = root.path("repository").path("full_name").asText(null);
        if (name == null) {
            name = root.path("repository").path("name").asText(null);
        }
        return name;
    }

    private Optional<Project> findProjectByRepoName(String repoName) {
        List<Project> allProjects = projectRepository.findAll();
        return allProjects.stream()
                .filter(p -> p.getRepositoryUrl() != null && (p.getRepositoryUrl().contains(repoName) || p.getName().equalsIgnoreCase(repoName)))
                .findFirst();
    }
}
