package com.devpilot.ai.cicd.controller;

import com.devpilot.ai.cicd.dto.PipelineCreateRequest;
import com.devpilot.ai.cicd.dto.PipelineResponse;
import com.devpilot.ai.cicd.dto.PipelineUpdateRequest;
import com.devpilot.ai.cicd.service.PipelineService;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/pipelines")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    private UUID resolveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PipelineResponse>>> getProjectPipelines(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PipelineResponse> response = pipelineService.getProjectPipelines(projectId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Pipelines retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PipelineResponse>> createPipeline(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PipelineCreateRequest request) {

        PipelineResponse response = pipelineService.createPipeline(projectId, resolveUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline created successfully"));
    }

    @GetMapping("/{pipelineId}")
    public ResponseEntity<ApiResponse<PipelineResponse>> getPipelineDetails(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PipelineResponse response = pipelineService.getPipelineDetails(projectId, pipelineId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline details retrieved successfully"));
    }

    @PutMapping("/{pipelineId}")
    public ResponseEntity<ApiResponse<PipelineResponse>> updatePipeline(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PipelineUpdateRequest request) {

        PipelineResponse response = pipelineService.updatePipeline(projectId, pipelineId, resolveUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline updated successfully"));
    }

    @DeleteMapping("/{pipelineId}")
    public ResponseEntity<ApiResponse<Void>> deletePipeline(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal UserPrincipal principal) {

        pipelineService.deletePipeline(projectId, pipelineId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(null, "Pipeline deleted successfully"));
    }
}
