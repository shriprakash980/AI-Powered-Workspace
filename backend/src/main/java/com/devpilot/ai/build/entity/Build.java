package com.devpilot.ai.build.entity;

import com.devpilot.ai.build.entity.enums.BuildMode;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.entity.enums.ProjectType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "builds")
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BuildStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 32)
    private ProjectType projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 32)
    private BuildMode mode;

    @Column(name = "command", length = 512)
    private String command;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "artifact_path", length = 512)
    private String artifactPath;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Build() {}

    public Build(UUID id, UUID projectId, UUID userId, BuildStatus status, ProjectType projectType, BuildMode mode, String command, String commitHash, String artifactPath, Integer exitCode, Instant startedAt, Instant completedAt, Long durationMs, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.status = status;
        this.projectType = projectType;
        this.mode = mode;
        this.command = command;
        this.commitHash = commitHash;
        this.artifactPath = artifactPath;
        this.exitCode = exitCode;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationMs = durationMs;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BuildStatus getStatus() { return status; }
    public void setStatus(BuildStatus status) { this.status = status; }

    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType projectType) { this.projectType = projectType; }

    public BuildMode getMode() { return mode; }
    public void setMode(BuildMode mode) { this.mode = mode; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getArtifactPath() { return artifactPath; }
    public void setArtifactPath(String artifactPath) { this.artifactPath = artifactPath; }

    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
