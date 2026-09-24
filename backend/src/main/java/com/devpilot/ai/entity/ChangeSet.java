package com.devpilot.ai.entity;

import com.devpilot.ai.entity.enums.ChangeSetStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_changesets")
public class ChangeSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ChangeSetStatus status = ChangeSetStatus.PROPOSED;

    @OneToMany(mappedBy = "changeSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<FileChange> fileChanges = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    public ChangeSet() {}

    public ChangeSet(UUID id, UUID projectId, UUID userId, UUID conversationId, String summary, ChangeSetStatus status) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.summary = summary;
        this.status = status != null ? status : ChangeSetStatus.PROPOSED;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public ChangeSetStatus getStatus() {
        return status;
    }

    public void setStatus(ChangeSetStatus status) {
        this.status = status;
    }

    public List<FileChange> getFileChanges() {
        return fileChanges;
    }

    public void setFileChanges(List<FileChange> fileChanges) {
        this.fileChanges = fileChanges;
    }

    public void addFileChange(FileChange change) {
        this.fileChanges.add(change);
        change.setChangeSet(this);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }
}
