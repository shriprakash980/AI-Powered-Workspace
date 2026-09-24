package com.devpilot.ai.dto.ai;

import java.time.Instant;
import java.util.UUID;

public class ConversationMessageResponse {

    private UUID id;
    private UUID conversationId;
    private String role;
    private String content;
    private UUID fileId;
    private Instant createdAt;

    public ConversationMessageResponse() {}

    public ConversationMessageResponse(UUID id, UUID conversationId, String role, String content, UUID fileId, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.fileId = fileId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
