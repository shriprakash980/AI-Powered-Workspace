package com.devpilot.ai.cicd.entity;

import com.devpilot.ai.cicd.model.PipelineStepStatus;
import com.devpilot.ai.cicd.model.PipelineStepType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_steps")
public class PipelineStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "pipeline_run_id", nullable = false)
    private UUID pipelineRunId;

    @Column(name = "step_name", nullable = false, length = 120)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 32)
    private PipelineStepType stepType;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PipelineStepStatus status = PipelineStepStatus.QUEUED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "log_reference", length = 255)
    private String logReference;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PipelineStep() {}

    public PipelineStep(UUID id, UUID pipelineRunId, String stepName, PipelineStepType stepType, int stepOrder, PipelineStepStatus status, Instant startedAt, Instant completedAt, Long durationMs, Integer exitCode, String logReference, String errorMessage, Instant createdAt) {
        this.id = id;
        this.pipelineRunId = pipelineRunId;
        this.stepName = stepName;
        this.stepType = stepType;
        this.stepOrder = stepOrder;
        this.status = status != null ? status : PipelineStepStatus.QUEUED;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationMs = durationMs;
        this.exitCode = exitCode;
        this.logReference = logReference;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPipelineRunId() { return pipelineRunId; }
    public void setPipelineRunId(UUID pipelineRunId) { this.pipelineRunId = pipelineRunId; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public PipelineStepType getStepType() { return stepType; }
    public void setStepType(PipelineStepType stepType) { this.stepType = stepType; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public PipelineStepStatus getStatus() { return status; }
    public void setStatus(PipelineStepStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

    public String getLogReference() { return logReference; }
    public void setLogReference(String logReference) { this.logReference = logReference; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
