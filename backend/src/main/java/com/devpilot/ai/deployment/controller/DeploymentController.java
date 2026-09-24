package com.devpilot.ai.deployment.controller;

import com.devpilot.ai.deployment.dto.*;
import com.devpilot.ai.deployment.service.DeploymentAiService;
import com.devpilot.ai.deployment.service.DeploymentLogService;
import com.devpilot.ai.deployment.service.DeploymentService;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;
    private final DeploymentLogService deploymentLogService;
    private final DeploymentAiService deploymentAiService;

    public DeploymentController(DeploymentService deploymentService, DeploymentLogService deploymentLogService, DeploymentAiService deploymentAiService) {
        this.deploymentService = deploymentService;
        this.deploymentLogService = deploymentLogService;
        this.deploymentAiService = deploymentAiService;
    }

    private UUID resolveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentResponse>> createDeployment(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeploymentRequest request) {

        User user = new User();
        user.setId(resolveUserId(principal));
        DeploymentResponse response = deploymentService.createDeployment(projectId, user, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment queued successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeploymentResponse>>> getProjectDeployments(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = new User();
        user.setId(resolveUserId(principal));
        Page<DeploymentResponse> response = deploymentService.getProjectDeployments(projectId, user, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Project deployments retrieved successfully"));
    }

    @GetMapping("/{deploymentId}")
    public ResponseEntity<ApiResponse<DeploymentResponse>> getDeploymentDetails(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        DeploymentResponse response = deploymentService.getDeploymentDetails(projectId, deploymentId, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment details retrieved successfully"));
    }

    @GetMapping("/{deploymentId}/logs")
    public ResponseEntity<ApiResponse<List<DeploymentLogDto>>> getDeploymentLogs(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        deploymentService.getDeploymentDetails(projectId, deploymentId, user);
        List<DeploymentLogDto> logs = deploymentLogService.getDeploymentLogs(deploymentId);
        return ResponseEntity.ok(ApiResponse.success(logs, "Deployment logs retrieved successfully"));
    }

    @PostMapping("/{deploymentId}/stop")
    public ResponseEntity<ApiResponse<DeploymentResponse>> stopDeployment(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        DeploymentResponse response = deploymentService.stopDeployment(projectId, deploymentId, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment stopped successfully"));
    }

    @PostMapping("/{deploymentId}/restart")
    public ResponseEntity<ApiResponse<DeploymentResponse>> restartDeployment(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        DeploymentResponse response = deploymentService.restartDeployment(projectId, deploymentId, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment restarted successfully"));
    }

    @PostMapping("/{deploymentId}/rollback")
    public ResponseEntity<ApiResponse<DeploymentResponse>> rollbackDeployment(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        DeploymentResponse response = deploymentService.rollbackDeployment(projectId, deploymentId, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment rolled back successfully"));
    }

    @PostMapping("/{deploymentId}/diagnose")
    public ResponseEntity<ApiResponse<DeploymentDiagnosisResponse>> diagnoseDeploymentFailure(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeploymentDiagnosisResponse response = deploymentAiService.diagnoseDeploymentFailure(projectId, deploymentId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment failure diagnosed by AI"));
    }
}
