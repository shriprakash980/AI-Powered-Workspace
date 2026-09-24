package com.devpilot.ai.cloud.dto;

import com.devpilot.ai.cloud.model.HealthStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ObservabilityMetricsResponse(
        UUID projectId,
        HealthStatus overallHealth,
        double cpuUsagePercent,
        double memoryUsagePercent,
        long activeDeploymentsCount,
        long totalRequests24h,
        long errorCount24h,
        double avgLatencyMs,
        Map<String, String> systemMetrics,
        List<String> activeAlerts,
        Instant timestamp
) {}
