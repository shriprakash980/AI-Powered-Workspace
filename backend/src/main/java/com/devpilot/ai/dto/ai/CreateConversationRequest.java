package com.devpilot.ai.dto.ai;

import java.util.UUID;

public class CreateConversationRequest {

    private UUID projectId;
    private String title;
    private String provider;
    private String model;

    public CreateConversationRequest() {}

    public CreateConversationRequest(UUID projectId, String title, String provider, String model) {
        this.projectId = projectId;
        this.title = title;
        this.provider = provider;
        this.model = model;
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
}
