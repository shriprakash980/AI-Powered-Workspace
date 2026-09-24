package com.devpilot.ai.deployment.entity;

import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_environment_variables")
public class ProjectEnvironmentVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 32)
    private DeploymentEnvironment environment = DeploymentEnvironment.DEVELOPMENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProjectEnvironmentVariable() {}

    public ProjectEnvironmentVariable(UUID id, UUID projectId, String name, String encryptedValue, DeploymentEnvironment environment, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.encryptedValue = encryptedValue;
        this.environment = environment != null ? environment : DeploymentEnvironment.DEVELOPMENT;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEncryptedValue() { return encryptedValue; }
    public void setEncryptedValue(String encryptedValue) { this.encryptedValue = encryptedValue; }

    public DeploymentEnvironment getEnvironment() { return environment; }
    public void setEnvironment(DeploymentEnvironment environment) { this.environment = environment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static ProjectEnvironmentVariableBuilder builder() {
        return new ProjectEnvironmentVariableBuilder();
    }

    public static class ProjectEnvironmentVariableBuilder {
        private UUID id;
        private UUID projectId;
        private String name;
        private String encryptedValue;
        private DeploymentEnvironment environment = DeploymentEnvironment.DEVELOPMENT;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public ProjectEnvironmentVariableBuilder id(UUID id) { this.id = id; return this; }
        public ProjectEnvironmentVariableBuilder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public ProjectEnvironmentVariableBuilder name(String name) { this.name = name; return this; }
        public ProjectEnvironmentVariableBuilder encryptedValue(String encryptedValue) { this.encryptedValue = encryptedValue; return this; }
        public ProjectEnvironmentVariableBuilder environment(DeploymentEnvironment environment) { this.environment = environment; return this; }
        public ProjectEnvironmentVariableBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public ProjectEnvironmentVariableBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectEnvironmentVariable build() {
            return new ProjectEnvironmentVariable(id, projectId, name, encryptedValue, environment, createdAt, updatedAt);
        }
    }
}
