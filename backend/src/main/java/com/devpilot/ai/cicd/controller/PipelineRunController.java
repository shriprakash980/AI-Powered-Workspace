package com.devpilot.ai.cicd.controller;

import com.devpilot.ai.cicd.dto.*;
import com.devpilot.ai.cicd.service.*;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/pipelines/{pipelineId}")
public class PipelineRunController {

    private final PipelineService pipelineService;
    private final PipelineRunService runService;
    private final PipelineLogService logService;
    private final PipelineConfigService configService;
    private final PipelineAiService aiService;

    public PipelineRunController(
            PipelineService pipelineService,
            PipelineRunService runService,
            PipelineLogService logService,
            PipelineConfigService configService,
            PipelineAiService aiService) {
        this.pipelineService = pipelineService;
        this.runService = runService;
        this.logService = logService;
        this.configService = configService;
        this.aiService = aiService;
    }

    private UUID resolveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }

    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<Page<PipelineRunResponse>>> getPipelineRuns(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PipelineRunResponse> response = runService.getPipelineRuns(projectId, pipelineId, resolveUserId(principal), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline runs retrieved successfully"));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ApiResponse<PipelineRunResponse>> getRunDetails(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @PathVariable UUID runId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PipelineRunResponse response = runService.getRunDetails(projectId, pipelineId, runId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline run details retrieved successfully"));
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<PipelineRunResponse>> triggerPipelineRun(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) PipelineTriggerRequest request) {

        PipelineRunResponse response = pipelineService.triggerPipelineManual(projectId, pipelineId, resolveUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline run queued successfully"));
    }

    @PostMapping("/runs/{runId}/cancel")
    public ResponseEntity<ApiResponse<PipelineRunResponse>> cancelPipelineRun(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @PathVariable UUID runId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PipelineRunResponse response = runService.cancelPipelineRun(projectId, pipelineId, runId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline run cancelled successfully"));
    }

    @PostMapping("/runs/{runId}/rerun")
    public ResponseEntity<ApiResponse<PipelineRunResponse>> rerunPipelineRun(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @PathVariable UUID runId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PipelineRunResponse response = runService.rerunPipelineRun(projectId, pipelineId, runId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline rerun queued successfully"));
    }

    @GetMapping("/runs/{runId}/steps")
    public ResponseEntity<ApiResponse<List<PipelineStepResponse>>> getRunSteps(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @PathVariable UUID runId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PipelineStepResponse> response = runService.getRunSteps(projectId, pipelineId, runId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline steps retrieved successfully"));
    }

    @GetMapping("/runs/{runId}/logs")
    public ResponseEntity<ApiResponse<PipelineLogResponse>> getRunLogs(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @PathVariable UUID runId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String stepName) {

        runService.getRunDetails(projectId, pipelineId, runId, resolveUserId(principal));
        PipelineLogResponse response = logService.getRunLogs(runId, stepName);
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline logs retrieved successfully"));
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPipelineConfig(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PipelineResponse pipeline = pipelineService.getPipelineDetails(projectId, pipelineId, resolveUserId(principal));
        String rawYaml = configService.loadRawConfig(projectId, pipeline.configPath());
        return ResponseEntity.ok(ApiResponse.success(Map.of("rawYaml", rawYaml), "Pipeline configuration retrieved successfully"));
    }

    @PostMapping("/runs/{runId}/diagnose")
    public ResponseEntity<ApiResponse<PipelineDiagnosisResponse>> diagnoseFailure(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @PathVariable UUID runId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PipelineDiagnosisResponse response = aiService.diagnosePipelineFailure(projectId, runId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline failure diagnosed by AI"));
    }
}
