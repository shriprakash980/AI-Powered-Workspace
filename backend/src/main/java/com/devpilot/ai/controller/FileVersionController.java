package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.patch.FileVersionResponse;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.FileVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/files/{fileId}/versions")
@Tag(name = "File Versions", description = "Endpoints for tracking, viewing, and restoring historical file snapshots")
@SecurityRequirement(name = "BearerAuth")
public class FileVersionController {

    private static final Logger log = LoggerFactory.getLogger(FileVersionController.class);

    private final FileVersionService fileVersionService;

    public FileVersionController(FileVersionService fileVersionService) {
        this.fileVersionService = fileVersionService;
    }

    @GetMapping
    @Operation(summary = "List file versions", description = "Retrieves version history for a project file, ordered newest first.")
    public ResponseEntity<ApiResponse<List<FileVersionResponse>>> getVersions(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to list versions for file {} in project {}", fileId, projectId);
        List<FileVersionResponse> response = fileVersionService.getVersions(projectId, fileId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "File versions retrieved successfully"));
    }

    @GetMapping("/{versionId}")
    @Operation(summary = "Get file version", description = "Retrieves a specific file version snapshot including content.")
    public ResponseEntity<ApiResponse<FileVersionResponse>> getVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to get version {} for file {} in project {}", versionId, fileId, projectId);
        FileVersionResponse response = fileVersionService.getVersion(projectId, fileId, versionId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "File version retrieved successfully"));
    }

    @PostMapping("/{versionId}/restore")
    @Operation(summary = "Restore file version", description = "Restores file content to a previous version and generates a new rollback snapshot.")
    public ResponseEntity<ApiResponse<FileVersionResponse>> restoreVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to restore version {} for file {} in project {}", versionId, fileId, projectId);
        FileVersionResponse response = fileVersionService.restoreVersion(projectId, fileId, versionId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "File version restored successfully"));
    }
}
