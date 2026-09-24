package com.devpilot.ai.patch;

import java.time.Instant;
import java.util.UUID;

public class FileChangeResponse {
    private UUID id;
    private UUID fileId;
    private String operation;
    private String oldPath;
    private String newPath;
    private String oldContentHash;
    private String proposedContentHash;
    private String originalContent;
    private String proposedContent;
    private String diff;
    private String status;
    private int additions;
    private int deletions;
    private Instant createdAt;
    private Instant appliedAt;

    public FileChangeResponse() {}

    public FileChangeResponse(UUID id, UUID fileId, String operation, String oldPath, String newPath, String oldContentHash, String proposedContentHash, String originalContent, String proposedContent, String diff, String status, int additions, int deletions, Instant createdAt, Instant appliedAt) {
        this.id = id;
        this.fileId = fileId;
        this.operation = operation;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.oldContentHash = oldContentHash;
        this.proposedContentHash = proposedContentHash;
        this.originalContent = originalContent;
        this.proposedContent = proposedContent;
        this.diff = diff;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.createdAt = createdAt;
        this.appliedAt = appliedAt;
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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getOldContentHash() {
        return oldContentHash;
    }

    public void setOldContentHash(String oldContentHash) {
        this.oldContentHash = oldContentHash;
    }

    public String getProposedContentHash() {
        return proposedContentHash;
    }

    public void setProposedContentHash(String proposedContentHash) {
        this.proposedContentHash = proposedContentHash;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getProposedContent() {
        return proposedContent;
    }

    public void setProposedContent(String proposedContent) {
        this.proposedContent = proposedContent;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAdditions() {
        return additions;
    }

    public void setAdditions(int additions) {
        this.additions = additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }
}
