package com.devpilot.ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContextResponse {

    private UUID projectId;
    private int totalFilesConsidered;
    private List<SelectedFileInfo> selectedFiles = new ArrayList<>();
    private List<ContextChunk> chunks = new ArrayList<>();
    private int estimatedCharacters;
    private int estimatedTokens;

    public ContextResponse() {}

    public ContextResponse(UUID projectId, int totalFilesConsidered, List<SelectedFileInfo> selectedFiles, List<ContextChunk> chunks, int estimatedCharacters, int estimatedTokens) {
        this.projectId = projectId;
        this.totalFilesConsidered = totalFilesConsidered;
        this.selectedFiles = selectedFiles != null ? selectedFiles : new ArrayList<>();
        this.chunks = chunks != null ? chunks : new ArrayList<>();
        this.estimatedCharacters = estimatedCharacters;
        this.estimatedTokens = estimatedTokens;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public int getTotalFilesConsidered() {
        return totalFilesConsidered;
    }

    public void setTotalFilesConsidered(int totalFilesConsidered) {
        this.totalFilesConsidered = totalFilesConsidered;
    }

    public List<SelectedFileInfo> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<SelectedFileInfo> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    public List<ContextChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<ContextChunk> chunks) {
        this.chunks = chunks;
    }

    public int getEstimatedCharacters() {
        return estimatedCharacters;
    }

    public void setEstimatedCharacters(int estimatedCharacters) {
        this.estimatedCharacters = estimatedCharacters;
    }

    public int getEstimatedTokens() {
        return estimatedTokens;
    }

    public void setEstimatedTokens(int estimatedTokens) {
        this.estimatedTokens = estimatedTokens;
    }

    public static class SelectedFileInfo {
        private UUID fileId;
        private String path;
        private int score;
        private String reason;

        public SelectedFileInfo() {}

        public SelectedFileInfo(UUID fileId, String path, int score, String reason) {
            this.fileId = fileId;
            this.path = path;
            this.score = score;
            this.reason = reason;
        }

        public UUID getFileId() {
            return fileId;
        }

        public void setFileId(UUID fileId) {
            this.fileId = fileId;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
