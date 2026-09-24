package com.devpilot.ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContextItem {
    private UUID fileId;
    private String path;
    private int score;
    private String reason;
    private String content;
    private String language;
    private List<ContextChunk> chunks = new ArrayList<>();

    public ContextItem() {}

    public ContextItem(UUID fileId, String path, int score, String reason, String content, String language) {
        this.fileId = fileId;
        this.path = path;
        this.score = score;
        this.reason = reason;
        this.content = content;
        this.language = language;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<ContextChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<ContextChunk> chunks) {
        this.chunks = chunks != null ? chunks : new ArrayList<>();
    }
}
