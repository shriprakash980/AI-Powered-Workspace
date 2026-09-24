package com.devpilot.ai.dto.project;

import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;

import java.time.Instant;
import java.util.UUID;

public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private ProjectTemplate template;
    private String language;
    private String framework;
    private ProjectStatus status;
    private UUID ownerId;
    private String repositoryUrl;
    private String deploymentUrl;
    private boolean gitEnabled;
    private String defaultBranch;
    private String gitProvider;
    private Instant lastFetchedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public ProjectResponse() {}

    public ProjectResponse(UUID id, String name, String description, ProjectTemplate template, String language,
                           String framework, ProjectStatus status, UUID ownerId, String repositoryUrl,
                           String deploymentUrl, boolean gitEnabled, String defaultBranch, String gitProvider,
                           Instant lastFetchedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.template = template;
        this.language = language;
        this.framework = framework;
        this.status = status;
        this.ownerId = ownerId;
        this.repositoryUrl = repositoryUrl;
        this.deploymentUrl = deploymentUrl;
        this.gitEnabled = gitEnabled;
        this.defaultBranch = defaultBranch;
        this.gitProvider = gitProvider;
        this.lastFetchedAt = lastFetchedAt;
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

    public boolean isGitEnabled() { return gitEnabled; }
    public void setGitEnabled(boolean gitEnabled) { this.gitEnabled = gitEnabled; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getGitProvider() { return gitProvider; }
    public void setGitProvider(String gitProvider) { this.gitProvider = gitProvider; }

    public Instant getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(Instant lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private UUID id;
        private String name;
        private String description;
        private ProjectTemplate template;
        private String language;
        private String framework;
        private ProjectStatus status;
        private UUID ownerId;
        private String repositoryUrl;
        private String deploymentUrl;
        private boolean gitEnabled;
        private String defaultBranch;
        private String gitProvider;
        private Instant lastFetchedAt;
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
        public Builder gitEnabled(boolean gitEnabled) { this.gitEnabled = gitEnabled; return this; }
        public Builder defaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; return this; }
        public Builder gitProvider(String gitProvider) { this.gitProvider = gitProvider; return this; }
        public Builder lastFetchedAt(Instant lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectResponse build() {
            return new ProjectResponse(id, name, description, template, language, framework, status, ownerId, repositoryUrl,
                    deploymentUrl, gitEnabled, defaultBranch, gitProvider, lastFetchedAt, createdAt, updatedAt);
        }
    }
}
