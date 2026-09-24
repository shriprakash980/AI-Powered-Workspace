package com.devpilot.ai.dto.ai;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class AICodeActionRequest {

    private UUID projectId;
    private UUID fileId;
    private String filePath;
    private String fileContent;

    @NotBlank(message = "Selected code cannot be empty")
    private String selectedCode;

    private String instruction;
    private String language;
    private String provider;
    private String model;

    public AICodeActionRequest() {}

    public AICodeActionRequest(UUID projectId, UUID fileId, String filePath, String fileContent, String selectedCode, String instruction, String language, String provider, String model) {
        this.projectId = projectId;
        this.fileId = fileId;
        this.filePath = filePath;
        this.fileContent = fileContent;
        this.selectedCode = selectedCode;
        this.instruction = instruction;
        this.language = language;
        this.provider = provider;
        this.model = model;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public String getSelectedCode() {
        return selectedCode;
    }

    public void setSelectedCode(String selectedCode) {
        this.selectedCode = selectedCode;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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
