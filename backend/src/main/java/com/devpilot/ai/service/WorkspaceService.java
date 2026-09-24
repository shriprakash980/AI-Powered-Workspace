package com.devpilot.ai.service;

import com.devpilot.ai.dto.file.FileNodeResponse;
import com.devpilot.ai.dto.file.ProjectFileResponse;
import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.dto.workspace.WorkspaceResponse;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    private final ProjectService projectService;
    private final ProjectFileService projectFileService;
    private final ProjectFileRepository projectFileRepository;
    private final ActivityLogService activityLogService;

    public WorkspaceService(ProjectService projectService,
                            ProjectFileService projectFileService,
                            ProjectFileRepository projectFileRepository,
                            ActivityLogService activityLogService) {
        this.projectService = projectService;
        this.projectFileService = projectFileService;
        this.projectFileRepository = projectFileRepository;
        this.activityLogService = activityLogService;
    }

    public WorkspaceResponse getWorkspace(UUID projectId, UserPrincipal userPrincipal) {
        log.info("Loading workspace metadata for project ID: {}", projectId);

        ProjectResponse project = projectService.getProjectById(projectId, userPrincipal);
        List<FileNodeResponse> tree = projectFileService.getFileTree(projectId, userPrincipal);

        List<ProjectFile> recentEntities = projectFileRepository
                .findTop10ByProjectIdAndIsDirectoryFalseOrderByUpdatedAtDesc(projectId);

        List<ProjectFileResponse> recentFiles = recentEntities.stream()
                .map(this::mapToFileResponse)
                .toList();

        long totalFiles = projectFileRepository.countByProjectId(projectId);

        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        activityLogService.logActivity(userId, projectId, "WORKSPACE_OPENED", "Opened workspace for project: " + project.getName());

        return WorkspaceResponse.builder()
                .project(project)
                .tree(tree)
                .recentFiles(recentFiles)
                .totalFiles(totalFiles)
                .build();
    }

    private ProjectFileResponse mapToFileResponse(ProjectFile file) {
        long size = file.getContent() != null ? file.getContent().getBytes(StandardCharsets.UTF_8).length : 0L;
        return ProjectFileResponse.builder()
                .id(file.getId())
                .projectId(file.getProjectId())
                .parentId(file.getParentId())
                .name(file.getName())
                .path(file.getPath())
                .fileType(file.getFileType())
                .isDirectory(file.isDirectory())
                .content(null) // Keep workspace response lightweight: omit content in recent files list
                .size(size)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
