package com.devpilot.ai.dto.file;

import java.time.Instant;
import java.util.UUID;

public class ProjectFileResponse {

    private UUID id;
    private UUID projectId;
    private UUID parentId;
    private String name;
    private String path;
    private String fileType;
    private boolean isDirectory;
    private String content;
    private long size;
    private Instant createdAt;
    private Instant updatedAt;

    public ProjectFileResponse() {}

    public ProjectFileResponse(UUID id, UUID projectId, UUID parentId, String name, String path,
                               String fileType, boolean isDirectory, String content, long size,
                               Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.fileType = fileType;
        this.isDirectory = isDirectory;
        this.content = content;
        this.size = size;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private UUID id;
        private UUID projectId;
        private UUID parentId;
        private String name;
        private String path;
        private String fileType;
        private boolean isDirectory;
        private String content;
        private long size;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public Builder parentId(UUID parentId) { this.parentId = parentId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder fileType(String fileType) { this.fileType = fileType; return this; }
        public Builder isDirectory(boolean isDirectory) { this.isDirectory = isDirectory; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder size(long size) { this.size = size; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectFileResponse build() {
            return new ProjectFileResponse(id, projectId, parentId, name, path, fileType, isDirectory, content, size, createdAt, updatedAt);
        }
    }
}
