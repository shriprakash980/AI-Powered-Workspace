package com.devpilot.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_files", uniqueConstraints = {
    @UniqueConstraint(name = "uq_project_file_path", columnNames = {"project_id", "path"})
})
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_directory", nullable = false)
    private boolean isDirectory = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProjectFile() {}

    public ProjectFile(UUID id, UUID projectId, UUID parentId, String name, String path,
                       String fileType, String content, boolean isDirectory,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.fileType = fileType;
        this.content = content;
        this.isDirectory = isDirectory;
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

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

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
        private String content;
        private boolean isDirectory = false;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public Builder parentId(UUID parentId) { this.parentId = parentId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder fileType(String fileType) { this.fileType = fileType; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder isDirectory(boolean isDirectory) { this.isDirectory = isDirectory; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectFile build() {
            return new ProjectFile(id, projectId, parentId, name, path, fileType, content, isDirectory, createdAt, updatedAt);
        }
    }
}
