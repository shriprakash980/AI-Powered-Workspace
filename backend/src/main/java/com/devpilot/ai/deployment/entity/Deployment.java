package com.devpilot.ai.deployment.entity;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "build_id")
    private UUID buildId;

    @Column(name = "artifact_id")
    private UUID artifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 32)
    private DeploymentEnvironment environment = DeploymentEnvironment.DEVELOPMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DeploymentStatus status = DeploymentStatus.QUEUED;

    @Column(name = "deployment_url", length = 512)
    private String deploymentUrl;

    @Column(name = "runtime", length = 64)
    private String runtime;

    @Column(name = "version", length = 64)
    private String version;

    @Column(name = "allocated_port")
    private Integer allocatedPort;

    @Column(name = "container_id", length = 128)
    private String containerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    public Deployment() {}

    public Deployment(UUID id, UUID projectId, UUID userId, UUID buildId, UUID artifactId, DeploymentEnvironment environment, DeploymentStatus status, String deploymentUrl, String runtime, String version, Integer allocatedPort, String containerId, Instant createdAt, Instant stoppedAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.buildId = buildId;
        this.artifactId = artifactId;
        this.environment = environment != null ? environment : DeploymentEnvironment.DEVELOPMENT;
        this.status = status != null ? status : DeploymentStatus.QUEUED;
        this.deploymentUrl = deploymentUrl;
        this.runtime = runtime;
        this.version = version;
        this.allocatedPort = allocatedPort;
        this.containerId = containerId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.stoppedAt = stoppedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getBuildId() { return buildId; }
    public void setBuildId(UUID buildId) { this.buildId = buildId; }

    public UUID getArtifactId() { return artifactId; }
    public void setArtifactId(UUID artifactId) { this.artifactId = artifactId; }

    public DeploymentEnvironment getEnvironment() { return environment; }
    public void setEnvironment(DeploymentEnvironment environment) { this.environment = environment; }

    public DeploymentStatus getStatus() { return status; }
    public void setStatus(DeploymentStatus status) { this.status = status; }

    public String getDeploymentUrl() { return deploymentUrl; }
    public void setDeploymentUrl(String deploymentUrl) { this.deploymentUrl = deploymentUrl; }

    public String getRuntime() { return runtime; }
    public void setRuntime(String runtime) { this.runtime = runtime; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Integer getAllocatedPort() { return allocatedPort; }
    public void setAllocatedPort(Integer allocatedPort) { this.allocatedPort = allocatedPort; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; }

    public static DeploymentBuilder builder() {
        return new DeploymentBuilder();
    }

    public static class DeploymentBuilder {
        private UUID id;
        private UUID projectId;
        private UUID userId;
        private UUID buildId;
        private UUID artifactId;
        private DeploymentEnvironment environment = DeploymentEnvironment.DEVELOPMENT;
        private DeploymentStatus status = DeploymentStatus.QUEUED;
        private String deploymentUrl;
        private String runtime;
        private String version;
        private Integer allocatedPort;
        private String containerId;
        private Instant createdAt = Instant.now();
        private Instant stoppedAt;

        public DeploymentBuilder id(UUID id) { this.id = id; return this; }
        public DeploymentBuilder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public DeploymentBuilder userId(UUID userId) { this.userId = userId; return this; }
        public DeploymentBuilder buildId(UUID buildId) { this.buildId = buildId; return this; }
        public DeploymentBuilder artifactId(UUID artifactId) { this.artifactId = artifactId; return this; }
        public DeploymentBuilder environment(DeploymentEnvironment environment) { this.environment = environment; return this; }
        public DeploymentBuilder status(DeploymentStatus status) { this.status = status; return this; }
        public DeploymentBuilder deploymentUrl(String deploymentUrl) { this.deploymentUrl = deploymentUrl; return this; }
        public DeploymentBuilder runtime(String runtime) { this.runtime = runtime; return this; }
        public DeploymentBuilder version(String version) { this.version = version; return this; }
        public DeploymentBuilder allocatedPort(Integer allocatedPort) { this.allocatedPort = allocatedPort; return this; }
        public DeploymentBuilder containerId(String containerId) { this.containerId = containerId; return this; }
        public DeploymentBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public DeploymentBuilder stoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; return this; }

        public Deployment build() {
            return new Deployment(id, projectId, userId, buildId, artifactId, environment, status, deploymentUrl, runtime, version, allocatedPort, containerId, createdAt, stoppedAt);
        }
    }
}
