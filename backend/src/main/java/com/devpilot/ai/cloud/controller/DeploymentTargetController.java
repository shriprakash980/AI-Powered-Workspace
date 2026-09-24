package com.devpilot.ai.cloud.controller;

import com.devpilot.ai.cloud.dto.ApprovalRequest;
import com.devpilot.ai.cloud.dto.DeploymentTargetRequest;
import com.devpilot.ai.cloud.dto.DeploymentTargetResponse;
import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.entity.ServiceHealthCheck;
import com.devpilot.ai.cloud.service.CloudDeploymentTargetService;
import com.devpilot.ai.cloud.service.DeploymentApprovalService;
import com.devpilot.ai.cloud.service.HealthCheckService;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/deployment-targets")
public class DeploymentTargetController {

    private final CloudDeploymentTargetService targetService;
    private final DeploymentApprovalService approvalService;
    private final HealthCheckService healthCheckService;

    public DeploymentTargetController(CloudDeploymentTargetService targetService,
                                      DeploymentApprovalService approvalService,
                                      HealthCheckService healthCheckService) {
        this.targetService = targetService;
        this.approvalService = approvalService;
        this.healthCheckService = healthCheckService;
    }

    private String resolveUsername(UserPrincipal principal) {
        return principal != null ? principal.getUsername() : "user";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeploymentTargetResponse>>> getProjectTargets(
            @PathVariable UUID projectId) {
        List<DeploymentTargetResponse> targets = targetService.getProjectTargets(projectId);
        return ResponseEntity.ok(ApiResponse.success(targets, "Deployment targets retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentTargetResponse>> createTarget(
            @PathVariable UUID projectId,
            @Valid @RequestBody DeploymentTargetRequest request) {
        DeploymentTargetResponse response = targetService.createDeploymentTarget(projectId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment target created successfully"));
    }

    @PostMapping("/{targetId}/approve")
    public ResponseEntity<ApiResponse<DeploymentTargetResponse>> approveTarget(
            @PathVariable UUID projectId,
            @PathVariable UUID targetId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) ApprovalRequest request) {

        String approver = (request != null && request.approvedBy() != null) ? request.approvedBy() : resolveUsername(principal);
        String notes = (request != null) ? request.notes() : "Approved by user";

        approvalService.approveDeployment(targetId, approver, notes);
        DeploymentTargetResponse response = targetService.getTargetResponse(targetId, projectId);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment target approved successfully"));
    }

    @PostMapping("/{targetId}/reject")
    public ResponseEntity<ApiResponse<DeploymentTargetResponse>> rejectTarget(
            @PathVariable UUID projectId,
            @PathVariable UUID targetId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) ApprovalRequest request) {

        String rejector = (request != null && request.approvedBy() != null) ? request.approvedBy() : resolveUsername(principal);
        String reason = (request != null) ? request.notes() : "Rejected by user";

        approvalService.rejectDeployment(targetId, rejector, reason);
        DeploymentTargetResponse response = targetService.getTargetResponse(targetId, projectId);
        return ResponseEntity.ok(ApiResponse.success(response, "Deployment target rejected"));
    }

    @PostMapping("/{targetId}/health-check")
    public ResponseEntity<ApiResponse<ServiceHealthCheck>> triggerHealthCheck(
            @PathVariable UUID projectId,
            @PathVariable UUID targetId) {

        DeploymentTarget target = targetService.getTargetEntity(targetId, projectId);
        ServiceHealthCheck check = healthCheckService.performHealthCheck(target);
        return ResponseEntity.ok(ApiResponse.success(check, "Health check completed successfully"));
    }
}
