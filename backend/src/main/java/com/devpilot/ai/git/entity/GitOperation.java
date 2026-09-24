package com.devpilot.ai.git.entity;

import com.devpilot.ai.git.entity.enums.GitOperationType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "git_operations")
public class GitOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 50)
    private GitOperationType operation;

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "commit_hash", length = 100)
    private String commitHash;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GitOperation() {}

    public GitOperation(UUID id, UUID projectId, UUID userId, GitOperationType operation,
                        String branch, String commitHash, String status, String message, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.operation = operation;
        this.branch = branch;
        this.commitHash = commitHash;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public GitOperationType getOperation() { return operation; }
    public void setOperation(GitOperationType operation) { this.operation = operation; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static class Builder {
        private UUID id;
        private UUID projectId;
        private UUID userId;
        private GitOperationType operation;
        private String branch;
        private String commitHash;
        private String status = "SUCCESS";
        private String message;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder operation(GitOperationType operation) { this.operation = operation; return this; }
        public Builder branch(String branch) { this.branch = branch; return this; }
        public Builder commitHash(String commitHash) { this.commitHash = commitHash; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public GitOperation build() {
            return new GitOperation(id, projectId, userId, operation, branch, commitHash, status, message, createdAt);
        }
    }
}
