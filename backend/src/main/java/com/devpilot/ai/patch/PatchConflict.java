package com.devpilot.ai.patch;

import java.util.UUID;

public class PatchConflict {
    private UUID fileId;
    private String filePath;
    private String expectedHash;
    private String actualHash;
    private String message;

    public PatchConflict() {}

    public PatchConflict(UUID fileId, String filePath, String expectedHash, String actualHash, String message) {
        this.fileId = fileId;
        this.filePath = filePath;
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
        this.message = message;
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

    public String getExpectedHash() {
        return expectedHash;
    }

    public void setExpectedHash(String expectedHash) {
        this.expectedHash = expectedHash;
    }

    public String getActualHash() {
        return actualHash;
    }

    public void setActualHash(String actualHash) {
        this.actualHash = actualHash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
