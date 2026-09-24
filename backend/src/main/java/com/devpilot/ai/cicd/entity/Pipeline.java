package com.devpilot.ai.cicd.entity;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipelines")
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "config_path", nullable = false, length = 255)
    private String configPath = "devpilot-ci.yml";

    @Column(name = "default_branch", nullable = false, length = 100)
    private String defaultBranch = "main";

    @Column(name = "trigger_on_push", nullable = false)
    private boolean triggerOnPush = true;

    @Column(name = "trigger_on_pull_request", nullable = false)
    private boolean triggerOnPullRequest = true;

    @Column(name = "allow_manual_trigger", nullable = false)
    private boolean allowManualTrigger = true;

    @Column(name = "auto_deploy_enabled", nullable = false)
    private boolean autoDeployEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_environment", nullable = false, length = 32)
    private DeploymentEnvironment deploymentEnvironment = DeploymentEnvironment.DEVELOPMENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Pipeline() {}

    public Pipeline(UUID id, UUID projectId, String name, String description, boolean enabled, String configPath, String defaultBranch, boolean triggerOnPush, boolean triggerOnPullRequest, boolean allowManualTrigger, boolean autoDeployEnabled, DeploymentEnvironment deploymentEnvironment, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.configPath = configPath != null ? configPath : "devpilot-ci.yml";
        this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
        this.triggerOnPush = triggerOnPush;
        this.triggerOnPullRequest = triggerOnPullRequest;
        this.allowManualTrigger = allowManualTrigger;
        this.autoDeployEnabled = autoDeployEnabled;
        this.deploymentEnvironment = deploymentEnvironment != null ? deploymentEnvironment : DeploymentEnvironment.DEVELOPMENT;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getConfigPath() { return configPath; }
    public void setConfigPath(String configPath) { this.configPath = configPath; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public boolean isTriggerOnPush() { return triggerOnPush; }
    public void setTriggerOnPush(boolean triggerOnPush) { this.triggerOnPush = triggerOnPush; }

    public boolean isTriggerOnPullRequest() { return triggerOnPullRequest; }
    public void setTriggerOnPullRequest(boolean triggerOnPullRequest) { this.triggerOnPullRequest = triggerOnPullRequest; }

    public boolean isAllowManualTrigger() { return allowManualTrigger; }
    public void setAllowManualTrigger(boolean allowManualTrigger) { this.allowManualTrigger = allowManualTrigger; }

    public boolean isAutoDeployEnabled() { return autoDeployEnabled; }
    public void setAutoDeployEnabled(boolean autoDeployEnabled) { this.autoDeployEnabled = autoDeployEnabled; }

    public DeploymentEnvironment getDeploymentEnvironment() { return deploymentEnvironment; }
    public void setDeploymentEnvironment(DeploymentEnvironment deploymentEnvironment) { this.deploymentEnvironment = deploymentEnvironment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
