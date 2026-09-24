package com.devpilot.ai.dto.ai;

import java.time.Instant;
import java.util.UUID;

public class ConversationResponse {

    private UUID id;
    private UUID projectId;
    private String title;
    private String provider;
    private String model;
    private int messageCount;
    private Instant createdAt;
    private Instant updatedAt;

    public ConversationResponse() {}

    public ConversationResponse(UUID id, UUID projectId, String title, String provider, String model, int messageCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.provider = provider;
        this.model = model;
        this.messageCount = messageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
