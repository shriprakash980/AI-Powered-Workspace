package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.file.*;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ProjectFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/files")
@Tag(name = "Project Files", description = "Endpoints for managing virtual files, folders, and directory hierarchy in a project workspace")
@SecurityRequirement(name = "BearerAuth")
public class ProjectFileController {

    private static final Logger log = LoggerFactory.getLogger(ProjectFileController.class);

    private final ProjectFileService projectFileService;

    public ProjectFileController(ProjectFileService projectFileService) {
        this.projectFileService = projectFileService;
    }

    @PostMapping("/folders")
    @Operation(summary = "Create folder", description = "Creates a new virtual directory within the project filesystem.")
    public ResponseEntity<ApiResponse<ProjectFileResponse>> createFolder(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateFolderRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to create folder '{}' in project {}", request.getName(), projectId);
        ProjectFileResponse response = projectFileService.createFolder(projectId, request, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Folder created successfully"));
    }

    @PostMapping
    @Operation(summary = "Create file", description = "Creates a new virtual file with optional initial content in the project.")
    public ResponseEntity<ApiResponse<ProjectFileResponse>> createFile(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateFileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to create file '{}' in project {}", request.getName(), projectId);
        ProjectFileResponse response = projectFileService.createFile(projectId, request, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "File created successfully"));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file details and content", description = "Retrieves file metadata, line size, and source content.")
    public ResponseEntity<ApiResponse<ProjectFileResponse>> getFileById(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to get file ID {} in project {}", fileId, projectId);
        ProjectFileResponse response = projectFileService.getFileById(projectId, fileId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "File retrieved successfully"));
    }

    @PutMapping("/{fileId}")
    @Operation(summary = "Update file content", description = "Saves new source code content into the target file.")
    public ResponseEntity<ApiResponse<ProjectFileResponse>> updateFileContent(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @Valid @RequestBody UpdateFileContentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to update content of file ID {} in project {}", fileId, projectId);
        ProjectFileResponse response = projectFileService.updateFileContent(projectId, fileId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "File updated successfully"));
    }

    @PatchMapping("/{fileId}")
    @Operation(summary = "Rename file or folder", description = "Renames a file or folder and updates descendant paths recursively if it is a directory.")
    public ResponseEntity<ApiResponse<ProjectFileResponse>> renameFile(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @Valid @RequestBody RenameFileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to rename file ID {} to '{}' in project {}", fileId, request.getName(), projectId);
        ProjectFileResponse response = projectFileService.renameFile(projectId, fileId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "Item renamed successfully"));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete file or folder", description = "Deletes a file or directory (and all child nodes recursively) from the project.")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to delete file/folder ID {} in project {}", fileId, projectId);
        projectFileService.deleteFile(projectId, fileId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.message("File or directory deleted successfully"));
    }

    @PatchMapping("/{fileId}/move")
    @Operation(summary = "Move file or folder", description = "Relocates a file or directory under a new parent directory while preventing circular hierarchies.")
    public ResponseEntity<ApiResponse<ProjectFileResponse>> moveFile(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @Valid @RequestBody MoveFileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to move file ID {} to parent {} in project {}", fileId, request.getParentId(), projectId);
        ProjectFileResponse response = projectFileService.moveFile(projectId, fileId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "Item moved successfully"));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get project file tree", description = "Returns the hierarchical tree structure of files and folders for the workspace file explorer.")
    public ResponseEntity<ApiResponse<List<FileNodeResponse>>> getFileTree(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to get file tree for project {}", projectId);
        List<FileNodeResponse> tree = projectFileService.getFileTree(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(tree, "File tree retrieved successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search files in project", description = "Searches for files and directories matching the query string in name or path.")
    public ResponseEntity<ApiResponse<List<ProjectFileResponse>>> searchFiles(
            @PathVariable UUID projectId,
            @RequestParam("query") String query,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to search files with query '{}' in project {}", query, projectId);
        List<ProjectFileResponse> results = projectFileService.searchFiles(projectId, query, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(results, "Files found successfully"));
    }
}
