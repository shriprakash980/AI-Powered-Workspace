package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "System telemetry and service health indicators")
public class HealthController {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Operation(summary = "Health check indicator (v1)", description = "Returns active runtime status of the DevPilot AI backend.")
    @GetMapping({"/api/v1/health", "/api/health"})
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        HealthResponse health = HealthResponse.builder()
                .status("UP")
                .service("DevPilot AI Backend")
                .version("1.0.0-rc")
                .environment(activeProfile)
                .build();

        return ResponseEntity.ok(ApiResponse.success(health, "DevPilot AI backend is running"));
    }
}
