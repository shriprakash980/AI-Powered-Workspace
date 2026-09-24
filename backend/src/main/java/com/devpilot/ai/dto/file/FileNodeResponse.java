package com.devpilot.ai.dto.file;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileNodeResponse {

    private UUID id;
    private UUID parentId;
    private String name;
    private String path;
    private String fileType;
    private boolean isDirectory;
    private List<FileNodeResponse> children = new ArrayList<>();

    public FileNodeResponse() {}

    public FileNodeResponse(UUID id, UUID parentId, String name, String path,
                            String fileType, boolean isDirectory, List<FileNodeResponse> children) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.fileType = fileType;
        this.isDirectory = isDirectory;
        this.children = children != null ? children : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public List<FileNodeResponse> getChildren() { return children; }
    public void setChildren(List<FileNodeResponse> children) { this.children = children; }

    public static class Builder {
        private UUID id;
        private UUID parentId;
        private String name;
        private String path;
        private String fileType;
        private boolean isDirectory;
        private List<FileNodeResponse> children = new ArrayList<>();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder parentId(UUID parentId) { this.parentId = parentId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder fileType(String fileType) { this.fileType = fileType; return this; }
        public Builder isDirectory(boolean isDirectory) { this.isDirectory = isDirectory; return this; }
        public Builder children(List<FileNodeResponse> children) { this.children = children; return this; }

        public FileNodeResponse build() {
            return new FileNodeResponse(id, parentId, name, path, fileType, isDirectory, children);
        }
    }
}
