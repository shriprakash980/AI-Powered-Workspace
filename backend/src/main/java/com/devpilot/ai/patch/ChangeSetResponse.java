package com.devpilot.ai.patch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChangeSetResponse {
    private UUID id;
    private UUID projectId;
    private UUID userId;
    private UUID conversationId;
    private String summary;
    private String status;
    private int filesCount;
    private List<FileChangeResponse> files = new ArrayList<>();
    private Instant createdAt;
    private Instant appliedAt;
    private Instant rejectedAt;

    public ChangeSetResponse() {}

    public ChangeSetResponse(UUID id, UUID projectId, UUID userId, UUID conversationId, String summary, String status, List<FileChangeResponse> files, Instant createdAt, Instant appliedAt, Instant rejectedAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.summary = summary;
        this.status = status;
        this.files = files != null ? files : new ArrayList<>();
        this.filesCount = this.files.size();
        this.createdAt = createdAt;
        this.appliedAt = appliedAt;
        this.rejectedAt = rejectedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public List<FileChangeResponse> getFiles() {
        return files;
    }

    public void setFiles(List<FileChangeResponse> files) {
        this.files = files != null ? files : new ArrayList<>();
        this.filesCount = this.files.size();
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
