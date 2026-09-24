package com.devpilot.ai.build.controller;

import com.devpilot.ai.build.dto.*;
import com.devpilot.ai.build.service.BuildAiService;
import com.devpilot.ai.build.service.BuildDetectionService;
import com.devpilot.ai.build.service.BuildLogService;
import com.devpilot.ai.build.service.BuildService;
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
@RequestMapping("/api/v1/projects/{projectId}/builds")
public class BuildController {

    private final BuildService buildService;
    private final BuildDetectionService buildDetectionService;
    private final BuildLogService buildLogService;
    private final BuildAiService buildAiService;

    public BuildController(BuildService buildService, BuildDetectionService buildDetectionService, BuildLogService buildLogService, BuildAiService buildAiService) {
        this.buildService = buildService;
        this.buildDetectionService = buildDetectionService;
        this.buildLogService = buildLogService;
        this.buildAiService = buildAiService;
    }

    private UUID resolveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }

    @PostMapping("/detect")
    public ResponseEntity<ApiResponse<ProjectDetectionResponse>> detectProject(@PathVariable UUID projectId) {
        ProjectDetectionResponse response = buildDetectionService.detectProjectType(projectId);
        return ResponseEntity.ok(ApiResponse.success(response, "Project build type detected successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BuildResponse>> triggerBuild(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BuildRequest request) {

        User user = new User();
        user.setId(resolveUserId(principal));
        BuildResponse response = buildService.triggerBuild(projectId, user, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Build queued successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BuildResponse>>> getProjectBuilds(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = new User();
        user.setId(resolveUserId(principal));
        Page<BuildResponse> response = buildService.getProjectBuilds(projectId, user, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Project builds retrieved successfully"));
    }

    @GetMapping("/{buildId}")
    public ResponseEntity<ApiResponse<BuildResponse>> getBuildDetails(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        BuildResponse response = buildService.getBuildDetailsByUuid(projectId, buildId, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Build details retrieved successfully"));
    }

    @GetMapping("/{buildId}/logs")
    public ResponseEntity<ApiResponse<List<BuildLogDto>>> getBuildLogs(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        buildService.getBuildDetailsByUuid(projectId, buildId, user);
        List<BuildLogDto> logs = buildLogService.getBuildLogs(buildId);
        return ResponseEntity.ok(ApiResponse.success(logs, "Build logs retrieved successfully"));
    }

    @PostMapping("/{buildId}/cancel")
    public ResponseEntity<ApiResponse<BuildResponse>> cancelBuild(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = new User();
        user.setId(resolveUserId(principal));
        BuildResponse response = buildService.cancelBuild(projectId, buildId, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Build cancelled successfully"));
    }

    @PostMapping("/{buildId}/diagnose")
    public ResponseEntity<ApiResponse<BuildDiagnosisResponse>> diagnoseBuildFailure(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId,
            @AuthenticationPrincipal UserPrincipal principal) {

        BuildDiagnosisResponse response = buildAiService.diagnoseBuildFailure(projectId, buildId, resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Build failure diagnosed by AI"));
    }
}
