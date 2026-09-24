package com.devpilot.ai.cicd.entity;

import com.devpilot.ai.cicd.model.PipelineRunStatus;
import com.devpilot.ai.cicd.model.PipelineTriggerType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_runs")
public class PipelineRun {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private PipelineTriggerType triggerType;

    @Column(name = "triggered_by", length = 120)
    private String triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PipelineRunStatus status = PipelineRunStatus.QUEUED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "artifact_id")
    private UUID artifactId;

    @Column(name = "deployment_id")
    private UUID deploymentId;

    @Column(name = "delivery_id", length = 120)
    private String deliveryId;

    @Column(name = "is_fork", nullable = false)
    private boolean isFork = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PipelineRun() {}

    public PipelineRun(UUID id, UUID pipelineId, UUID projectId, String commitSha, String branch, String commitMessage, PipelineTriggerType triggerType, String triggeredBy, PipelineRunStatus status, Instant startedAt, Instant completedAt, Long durationMs, String errorMessage, UUID artifactId, UUID deploymentId, String deliveryId, boolean isFork, Instant createdAt) {
        this.id = id;
        this.pipelineId = pipelineId;
        this.projectId = projectId;
        this.commitSha = commitSha;
        this.branch = branch;
        this.commitMessage = commitMessage;
        this.triggerType = triggerType;
        this.triggeredBy = triggeredBy;
        this.status = status != null ? status : PipelineRunStatus.QUEUED;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.artifactId = artifactId;
        this.deploymentId = deploymentId;
        this.deliveryId = deliveryId;
        this.isFork = isFork;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPipelineId() { return pipelineId; }
    public void setPipelineId(UUID pipelineId) { this.pipelineId = pipelineId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }

    public PipelineTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(PipelineTriggerType triggerType) { this.triggerType = triggerType; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public PipelineRunStatus getStatus() { return status; }
    public void setStatus(PipelineRunStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public UUID getArtifactId() { return artifactId; }
    public void setArtifactId(UUID artifactId) { this.artifactId = artifactId; }

    public UUID getDeploymentId() { return deploymentId; }
    public void setDeploymentId(UUID deploymentId) { this.deploymentId = deploymentId; }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public boolean isFork() { return isFork; }
    public void setFork(boolean fork) { isFork = fork; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
