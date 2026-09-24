package com.devpilot.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ActivityLog() {}

    public ActivityLog(UUID id, UUID userId, UUID projectId, String action, String metadata, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.projectId = projectId;
        this.action = action;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private UUID projectId;
        private String action;
        private String metadata;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public ActivityLog build() {
            return new ActivityLog(id, userId, projectId, action, metadata, createdAt);
        }
    }
}
