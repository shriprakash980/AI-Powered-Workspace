package com.devpilot.ai.cloud.controller;

import com.devpilot.ai.cloud.dto.ObservabilityMetricsResponse;
import com.devpilot.ai.cloud.entity.DeploymentEvent;
import com.devpilot.ai.cloud.entity.ServiceHealthCheck;
import com.devpilot.ai.cloud.service.HealthCheckService;
import com.devpilot.ai.cloud.service.ObservabilityService;
import com.devpilot.ai.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/observability")
public class ObservabilityController {

    private final ObservabilityService observabilityService;
    private final HealthCheckService healthCheckService;

    public ObservabilityController(ObservabilityService observabilityService,
                                   HealthCheckService healthCheckService) {
        this.observabilityService = observabilityService;
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<ObservabilityMetricsResponse>> getMetrics(@PathVariable UUID projectId) {
        ObservabilityMetricsResponse metrics = observabilityService.getProjectMetrics(projectId);
        return ResponseEntity.ok(ApiResponse.success(metrics, "Observability metrics retrieved successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<List<ServiceHealthCheck>>> getHealthHistory(@PathVariable UUID projectId) {
        List<ServiceHealthCheck> history = healthCheckService.getHealthCheckHistory(projectId);
        return ResponseEntity.ok(ApiResponse.success(history, "Service health check history retrieved successfully"));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<DeploymentEvent>>> getDeploymentEvents(@PathVariable UUID projectId) {
        List<DeploymentEvent> events = observabilityService.getDeploymentEvents(projectId);
        return ResponseEntity.ok(ApiResponse.success(events, "Deployment events timeline retrieved successfully"));
    }
}
