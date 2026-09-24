package com.devpilot.ai.deployment.controller;

import com.devpilot.ai.deployment.dto.EnvironmentVariableRequest;
import com.devpilot.ai.deployment.dto.EnvironmentVariableResponse;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.service.EnvironmentVariableService;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/environment-variables")
public class EnvironmentVariableController {

    private final EnvironmentVariableService environmentVariableService;

    public EnvironmentVariableController(EnvironmentVariableService environmentVariableService) {
        this.environmentVariableService = environmentVariableService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EnvironmentVariableResponse>>> getEnvironmentVariables(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) DeploymentEnvironment environment) {

        List<EnvironmentVariableResponse> response = environmentVariableService.getProjectVariables(projectId, principal.getId(), environment);
        return ResponseEntity.ok(ApiResponse.success(response, "Environment variables retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EnvironmentVariableResponse>> saveEnvironmentVariable(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EnvironmentVariableRequest request) {

        EnvironmentVariableResponse response = environmentVariableService.saveVariable(projectId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Environment variable saved successfully"));
    }

    @DeleteMapping("/{variableId}")
    public ResponseEntity<ApiResponse<Void>> deleteEnvironmentVariable(
            @PathVariable UUID projectId,
            @PathVariable UUID variableId,
            @AuthenticationPrincipal UserPrincipal principal) {

        environmentVariableService.deleteVariable(projectId, variableId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Environment variable deleted successfully"));
    }
}
