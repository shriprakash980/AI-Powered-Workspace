package com.devpilot.ai.patch;

import java.time.Instant;
import java.util.UUID;

public class FileVersionResponse {
    private UUID id;
    private UUID fileId;
    private int versionNumber;
    private String content;
    private String contentHash;
    private String source;
    private UUID createdBy;
    private Instant createdAt;

    public FileVersionResponse() {}

    public FileVersionResponse(UUID id, UUID fileId, int versionNumber, String content, String contentHash, String source, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.fileId = fileId;
        this.versionNumber = versionNumber;
        this.content = content;
        this.contentHash = contentHash;
        this.source = source;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
