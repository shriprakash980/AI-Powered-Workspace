package com.devpilot.ai.cloud.entity;

import com.devpilot.ai.cloud.model.HealthStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_health_checks")
public class ServiceHealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "deployment_id", nullable = false)
    private UUID deploymentId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private HealthStatus status = HealthStatus.UNKNOWN;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "health_check_url", length = 512)
    private String healthCheckUrl;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt = Instant.now();

    public ServiceHealthCheck() {}

    public ServiceHealthCheck(UUID id, UUID deploymentId, UUID projectId, HealthStatus status, Long responseTimeMs, String healthCheckUrl, String details, Instant checkedAt) {
        this.id = id;
        this.deploymentId = deploymentId;
        this.projectId = projectId;
        this.status = status != null ? status : HealthStatus.UNKNOWN;
        this.responseTimeMs = responseTimeMs;
        this.healthCheckUrl = healthCheckUrl;
        this.details = details;
        this.checkedAt = checkedAt != null ? checkedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDeploymentId() { return deploymentId; }
    public void setDeploymentId(UUID deploymentId) { this.deploymentId = deploymentId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public HealthStatus getStatus() { return status; }
    public void setStatus(HealthStatus status) { this.status = status; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public String getHealthCheckUrl() { return healthCheckUrl; }
    public void setHealthCheckUrl(String healthCheckUrl) { this.healthCheckUrl = healthCheckUrl; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
}
