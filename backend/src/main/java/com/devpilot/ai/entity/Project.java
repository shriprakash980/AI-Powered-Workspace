package com.devpilot.ai.entity;

import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "template", nullable = false, length = 60)
    private ProjectTemplate template = ProjectTemplate.HTML_CSS_JS;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Column(name = "framework", length = 50)
    private String framework;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    @Column(name = "deployment_url", length = 500)
    private String deploymentUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Project() {}

    public Project(UUID id, String name, String description, ProjectTemplate template, String language,
                   String framework, ProjectStatus status, UUID ownerId, String repositoryUrl,
                   String deploymentUrl, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.template = template != null ? template : ProjectTemplate.HTML_CSS_JS;
        this.language = language;
        this.framework = framework;
        this.status = status != null ? status : ProjectStatus.ACTIVE;
        this.ownerId = ownerId;
        this.repositoryUrl = repositoryUrl;
        this.deploymentUrl = deploymentUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProjectTemplate getTemplate() { return template; }
    public void setTemplate(ProjectTemplate template) { this.template = template; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getDeploymentUrl() { return deploymentUrl; }
    public void setDeploymentUrl(String deploymentUrl) { this.deploymentUrl = deploymentUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private UUID id;
        private String name;
        private String description;
        private ProjectTemplate template = ProjectTemplate.HTML_CSS_JS;
        private String language;
        private String framework;
        private ProjectStatus status = ProjectStatus.ACTIVE;
        private UUID ownerId;
        private String repositoryUrl;
        private String deploymentUrl;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder template(ProjectTemplate template) { this.template = template; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder framework(String framework) { this.framework = framework; return this; }
        public Builder status(ProjectStatus status) { this.status = status; return this; }
        public Builder ownerId(UUID ownerId) { this.ownerId = ownerId; return this; }
        public Builder repositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; return this; }
        public Builder deploymentUrl(String deploymentUrl) { this.deploymentUrl = deploymentUrl; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Project build() {
            return new Project(id, name, description, template, language, framework, status, ownerId, repositoryUrl, deploymentUrl, createdAt, updatedAt);
        }
    }
}
