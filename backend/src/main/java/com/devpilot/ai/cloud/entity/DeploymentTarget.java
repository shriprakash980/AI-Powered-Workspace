package com.devpilot.ai.cloud.entity;

import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.model.CloudTargetStatus;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deployment_targets")
public class DeploymentTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 120)
    private String name = "Default Target";

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private CloudProviderType provider = CloudProviderType.LOCAL_DOCKER;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 32)
    private DeploymentEnvironment environment = DeploymentEnvironment.DEVELOPMENT;

    @Column(name = "credential_id")
    private UUID credentialId;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "service_name", length = 120)
    private String serviceName;

    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CloudTargetStatus status = CloudTargetStatus.INACTIVE;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired = false;

    @Column(name = "approved", nullable = false)
    private boolean approved = true;

    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "last_health_check_at")
    private Instant lastHealthCheckAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DeploymentTarget() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public CloudProviderType getProvider() { return provider; }
    public void setProvider(CloudProviderType provider) { this.provider = provider; }

    public DeploymentEnvironment getEnvironment() { return environment; }
    public void setEnvironment(DeploymentEnvironment environment) { this.environment = environment; }

    public UUID getCredentialId() { return credentialId; }
    public void setCredentialId(UUID credentialId) { this.credentialId = credentialId; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public CloudTargetStatus getStatus() { return status; }
    public void setStatus(CloudTargetStatus status) { this.status = status; }

    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }
    public String getConfigurationJson() { return configuration; }
    public void setConfigurationJson(String configurationJson) { this.configuration = configurationJson; }

    public Instant getLastHealthCheckAt() { return lastHealthCheckAt; }
    public void setLastHealthCheckAt(Instant lastHealthCheckAt) { this.lastHealthCheckAt = lastHealthCheckAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
