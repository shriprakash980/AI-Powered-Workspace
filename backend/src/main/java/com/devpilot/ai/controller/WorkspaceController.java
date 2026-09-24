package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.workspace.WorkspaceResponse;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/workspace")
@Tag(name = "Workspace", description = "Endpoints for loading developer workspace session metadata, file trees, and project state")
@SecurityRequirement(name = "BearerAuth")
public class WorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    @Operation(summary = "Get workspace details", description = "Retrieves project metadata, hierarchical file tree, and recent files for initializing the IDE workspace.")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to get workspace metadata for project {}", projectId);
        WorkspaceResponse response = workspaceService.getWorkspace(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "Workspace loaded successfully"));
    }
}
