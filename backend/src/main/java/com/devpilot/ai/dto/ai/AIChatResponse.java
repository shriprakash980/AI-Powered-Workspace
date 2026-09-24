package com.devpilot.ai.dto.ai;

import java.time.Instant;
import java.util.UUID;

public class AIChatResponse {

    private UUID conversationId;
    private UUID messageId;
    private String role;
    private String content;
    private String provider;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long latencyMs;
    private Instant createdAt;

    public AIChatResponse() {}

    public AIChatResponse(UUID conversationId, UUID messageId, String role, String content, String provider, String model, Integer promptTokens, Integer completionTokens, Long latencyMs, Instant createdAt) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.provider = provider;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.latencyMs = latencyMs;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
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

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
