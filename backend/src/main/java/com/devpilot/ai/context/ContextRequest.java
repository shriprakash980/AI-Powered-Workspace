package com.devpilot.ai.context;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ContextRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID fileId;
    private String selectedCode;
    private Integer startLine;
    private Integer endLine;
    private String query;
    private Integer maxFiles;
    private Integer maxChars;

    public ContextRequest() {}

    public ContextRequest(UUID projectId, UUID fileId, String selectedCode, Integer startLine, Integer endLine, String query) {
        this.projectId = projectId;
        this.fileId = fileId;
        this.selectedCode = selectedCode;
        this.startLine = startLine;
        this.endLine = endLine;
        this.query = query;
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

    public String getSelectedCode() {
        return selectedCode;
    }

    public void setSelectedCode(String selectedCode) {
        this.selectedCode = selectedCode;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(Integer maxFiles) {
        this.maxFiles = maxFiles;
    }

    public Integer getMaxChars() {
        return maxChars;
    }

    public void setMaxChars(Integer maxChars) {
        this.maxChars = maxChars;
    }
}
