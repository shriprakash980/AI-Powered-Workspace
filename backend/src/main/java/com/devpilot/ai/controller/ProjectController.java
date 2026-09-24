package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.project.ProjectRequest;
import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Endpoints for managing DevPilot AI cloud workspaces and repositories")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "List all active projects", description = "Retrieves all active cloud workspaces belonging to the authenticated workspace context.")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects() {
        log.info("REST request to get all active projects");
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(ApiResponse.success(projects, "Projects retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Retrieves metadata and configuration details of a specific project workspace.")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(@PathVariable UUID id) {
        log.info("REST request to get project by ID: {}", id);
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(project, "Project retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a new project", description = "Initializes and scaffolds a new project workspace based on the chosen template and language.")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectRequest request) {
        log.info("REST request to create new project: '{}'", request.getName());
        ProjectResponse project = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(project, "Project created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project configuration", description = "Updates metadata, template, or description for an existing workspace.")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request) {
        log.info("REST request to update project ID: {}", id);
        ProjectResponse updated = projectService.updateProject(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Project updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Soft deletes a project workspace.")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable UUID id) {
        log.info("REST request to delete project ID: {}", id);
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.message("Project deleted successfully"));
    }
}
