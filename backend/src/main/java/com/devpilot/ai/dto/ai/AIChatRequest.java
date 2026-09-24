package com.devpilot.ai.dto.ai;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class AIChatRequest {

    private UUID conversationId;
    private UUID projectId;
    private UUID fileId;

    @NotBlank(message = "Message content is required")
    private String message;

    private String provider;
    private String model;
    private String selectedCode;
    private String language;
    private Boolean stream = false;

    public AIChatRequest() {}

    public AIChatRequest(UUID conversationId, UUID projectId, UUID fileId, String message, String provider, String model, String selectedCode, String language, Boolean stream) {
        this.conversationId = conversationId;
        this.projectId = projectId;
        this.fileId = fileId;
        this.message = message;
        this.provider = provider;
        this.model = model;
        this.selectedCode = selectedCode;
        this.language = language;
        this.stream = stream != null ? stream : false;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getSelectedCode() {
        return selectedCode;
    }

    public void setSelectedCode(String selectedCode) {
        this.selectedCode = selectedCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream != null ? stream : false;
    }
}
