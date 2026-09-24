package com.devpilot.ai.service;

import com.devpilot.ai.dto.file.*;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ConflictException;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class ProjectFileService {

    private static final Logger log = LoggerFactory.getLogger(ProjectFileService.class);
    public static final int MAX_FILE_SIZE_BYTES = 1_000_000; // 1 MB limit

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ActivityLogService activityLogService;

    public ProjectFileService(ProjectRepository projectRepository,
                              ProjectFileRepository projectFileRepository,
                              ActivityLogService activityLogService) {
        this.projectRepository = projectRepository;
        this.projectFileRepository = projectFileRepository;
        this.activityLogService = activityLogService;
    }

    public ProjectFileResponse createFolder(UUID projectId, CreateFolderRequest request, UserPrincipal userPrincipal) {
        Project project = verifyProjectAccess(projectId, userPrincipal);
        String name = validateAndSanitizeName(request.getName());

        String path;
        UUID parentId = request.getParentId();
        if (parentId != null) {
            ProjectFile parent = findFileOrThrow(projectId, parentId);
            if (!parent.isDirectory()) {
                throw new BadRequestException("Parent node is not a directory");
            }
            path = parent.getPath() + "/" + name;
        } else {
            path = name;
        }

        validatePathSafety(path);

        if (projectFileRepository.existsByProjectIdAndPath(projectId, path)) {
            throw new ConflictException("A file or folder with path '" + path + "' already exists");
        }

        Instant now = Instant.now();
        ProjectFile folder = ProjectFile.builder()
                .projectId(projectId)
                .parentId(parentId)
                .name(name)
                .path(path)
                .fileType("directory")
                .content(null)
                .isDirectory(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ProjectFile saved = projectFileRepository.save(folder);
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        activityLogService.logActivity(userId, projectId, "FOLDER_CREATED", "Created folder: " + path);
        log.info("Created folder ID: {} at path '{}' in project {}", saved.getId(), path, projectId);

        return mapToFileResponse(saved);
    }

    public ProjectFileResponse createFile(UUID projectId, CreateFileRequest request, UserPrincipal userPrincipal) {
        Project project = verifyProjectAccess(projectId, userPrincipal);
        String name = validateAndSanitizeName(request.getName());
        validateContentSize(request.getContent());

        String path;
        UUID parentId = request.getParentId();
        if (parentId != null) {
            ProjectFile parent = findFileOrThrow(projectId, parentId);
            if (!parent.isDirectory()) {
                throw new BadRequestException("Parent node is not a directory");
            }
            path = parent.getPath() + "/" + name;
        } else {
            path = name;
        }

        validatePathSafety(path);

        if (projectFileRepository.existsByProjectIdAndPath(projectId, path)) {
            throw new ConflictException("A file or folder with path '" + path + "' already exists");
        }

        Instant now = Instant.now();
        String fileType = determineFileType(name);
        String content = request.getContent() != null ? request.getContent() : "";

        ProjectFile file = ProjectFile.builder()
                .projectId(projectId)
                .parentId(parentId)
                .name(name)
                .path(path)
                .fileType(fileType)
                .content(content)
                .isDirectory(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ProjectFile saved = projectFileRepository.save(file);
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        activityLogService.logActivity(userId, projectId, "FILE_CREATED", "Created file: " + path);
        log.info("Created file ID: {} at path '{}' in project {}", saved.getId(), path, projectId);

        return mapToFileResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectFileResponse getFileById(UUID projectId, UUID fileId, UserPrincipal userPrincipal) {
        verifyProjectAccess(projectId, userPrincipal);
        ProjectFile file = findFileOrThrow(projectId, fileId);
        return mapToFileResponse(file);
    }

    public ProjectFileResponse updateFileContent(UUID projectId, UUID fileId, UpdateFileContentRequest request, UserPrincipal userPrincipal) {
        verifyProjectAccess(projectId, userPrincipal);
        ProjectFile file = findFileOrThrow(projectId, fileId);

        if (file.isDirectory()) {
            throw new BadRequestException("Cannot update content of a directory");
        }

        validateContentSize(request.getContent());
        file.setContent(request.getContent() != null ? request.getContent() : "");
        file.setUpdatedAt(Instant.now());

        ProjectFile updated = projectFileRepository.save(file);
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        activityLogService.logActivity(userId, projectId, "FILE_UPDATED", "Updated file: " + file.getPath());
        log.info("Updated content for file ID: {} in project {}", fileId, projectId);

        return mapToFileResponse(updated);
    }

    public ProjectFileResponse renameFile(UUID projectId, UUID fileId, RenameFileRequest request, UserPrincipal userPrincipal) {
        verifyProjectAccess(projectId, userPrincipal);
        String newName = validateAndSanitizeName(request.getName());
        ProjectFile file = findFileOrThrow(projectId, fileId);

        if (file.getName().equals(newName)) {
            return mapToFileResponse(file);
        }

        String newPath;
        if (file.getParentId() == null) {
            newPath = newName;
        } else {
            ProjectFile parent = findFileOrThrow(projectId, file.getParentId());
            newPath = parent.getPath() + "/" + newName;
        }

        validatePathSafety(newPath);

        if (projectFileRepository.existsByProjectIdAndPath(projectId, newPath)) {
            throw new ConflictException("A file or folder with path '" + newPath + "' already exists");
        }

        Instant now = Instant.now();
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;

        if (!file.isDirectory()) {
            file.setName(newName);
            file.setPath(newPath);
            file.setFileType(determineFileType(newName));
            file.setUpdatedAt(now);
            ProjectFile updated = projectFileRepository.save(file);
            activityLogService.logActivity(userId, projectId, "FILE_RENAMED", "Renamed file to: " + newPath);
            return mapToFileResponse(updated);
        } else {
            String oldPrefix = file.getPath() + "/";
            String newPrefix = newPath + "/";
            List<ProjectFile> descendants = projectFileRepository.findByProjectIdAndPathStartingWith(projectId, oldPrefix);
            for (ProjectFile descendant : descendants) {
                String relative = descendant.getPath().substring(oldPrefix.length());
                descendant.setPath(newPrefix + relative);
                descendant.setUpdatedAt(now);
            }
            if (!descendants.isEmpty()) {
                projectFileRepository.saveAll(descendants);
            }

            file.setName(newName);
            file.setPath(newPath);
            file.setUpdatedAt(now);
            ProjectFile updated = projectFileRepository.save(file);
            activityLogService.logActivity(userId, projectId, "FOLDER_RENAMED", "Renamed folder to: " + newPath);
            return mapToFileResponse(updated);
        }
    }

    public void deleteFile(UUID projectId, UUID fileId, UserPrincipal userPrincipal) {
        verifyProjectAccess(projectId, userPrincipal);
        ProjectFile file = findFileOrThrow(projectId, fileId);
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;

        if (file.isDirectory()) {
            String prefix = file.getPath() + "/";
            List<ProjectFile> descendants = projectFileRepository.findByProjectIdAndPathStartingWith(projectId, prefix);
            if (!descendants.isEmpty()) {
                projectFileRepository.deleteAll(descendants);
            }
            projectFileRepository.delete(file);
            activityLogService.logActivity(userId, projectId, "FOLDER_DELETED", "Deleted folder: " + file.getPath());
            log.info("Deleted folder ID: {} and {} descendants in project {}", fileId, descendants.size(), projectId);
        } else {
            projectFileRepository.delete(file);
            activityLogService.logActivity(userId, projectId, "FILE_DELETED", "Deleted file: " + file.getPath());
            log.info("Deleted file ID: {} at '{}' in project {}", fileId, file.getPath(), projectId);
        }
    }

    public ProjectFileResponse moveFile(UUID projectId, UUID fileId, MoveFileRequest request, UserPrincipal userPrincipal) {
        verifyProjectAccess(projectId, userPrincipal);
        ProjectFile file = findFileOrThrow(projectId, fileId);
        UUID newParentId = request.getParentId();

        if (Objects.equals(file.getParentId(), newParentId)) {
            return mapToFileResponse(file);
        }

        String newPath;
        if (newParentId == null) {
            newPath = file.getName();
        } else {
            ProjectFile targetParent = findFileOrThrow(projectId, newParentId);
            if (!targetParent.isDirectory()) {
                throw new BadRequestException("Target destination is not a directory");
            }
            if (file.isDirectory()) {
                if (targetParent.getId().equals(file.getId())) {
                    throw new BadRequestException("Cannot move a folder into itself");
                }
                if (targetParent.getPath().equals(file.getPath()) || targetParent.getPath().startsWith(file.getPath() + "/")) {
                    throw new BadRequestException("Cannot move a folder into one of its subdirectories");
                }
            }
            newPath = targetParent.getPath() + "/" + file.getName();
        }

        validatePathSafety(newPath);

        if (projectFileRepository.existsByProjectIdAndPath(projectId, newPath)) {
            throw new ConflictException("A file or folder with path '" + newPath + "' already exists");
        }

        Instant now = Instant.now();
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;

        if (file.isDirectory()) {
            String oldPrefix = file.getPath() + "/";
            String newPrefix = newPath + "/";
            List<ProjectFile> descendants = projectFileRepository.findByProjectIdAndPathStartingWith(projectId, oldPrefix);
            for (ProjectFile descendant : descendants) {
                String relative = descendant.getPath().substring(oldPrefix.length());
                descendant.setPath(newPrefix + relative);
                descendant.setUpdatedAt(now);
            }
            if (!descendants.isEmpty()) {
                projectFileRepository.saveAll(descendants);
            }
        }

        file.setParentId(newParentId);
        file.setPath(newPath);
        file.setUpdatedAt(now);
        ProjectFile updated = projectFileRepository.save(file);

        activityLogService.logActivity(userId, projectId, "FILE_MOVED", "Moved item to: " + newPath);
        log.info("Moved file/folder ID: {} to '{}' in project {}", fileId, newPath, projectId);

        return mapToFileResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<FileNodeResponse> getFileTree(UUID projectId, UserPrincipal userPrincipal) {
        verifyProjectAccess(projectId, userPrincipal);
        List<ProjectFile> allFiles = projectFileRepository.findByProjectId(projectId);
        return buildFileTree(allFiles);
    }

    @Transactional(readOnly = true)
    public List<ProjectFileResponse> searchFiles(UUID projectId, String query, UserPrincipal userPrincipal) {
        verifyProjectAccess(projectId, userPrincipal);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String trimmedQuery = query.trim();
        List<ProjectFile> files = projectFileRepository
                .findByProjectIdAndNameContainingIgnoreCaseOrProjectIdAndPathContainingIgnoreCase(
                        projectId, trimmedQuery, projectId, trimmedQuery);

        return files.stream()
                .map(this::mapToFileResponse)
                .sorted(Comparator.comparing(ProjectFileResponse::getPath))
                .toList();
    }

    public List<FileNodeResponse> buildFileTree(List<ProjectFile> allFiles) {
        Map<UUID, FileNodeResponse> nodeMap = new HashMap<>();
        Map<UUID, List<FileNodeResponse>> childrenByParentId = new HashMap<>();
        List<FileNodeResponse> rootNodes = new ArrayList<>();

        for (ProjectFile file : allFiles) {
            FileNodeResponse node = FileNodeResponse.builder()
                    .id(file.getId())
                    .parentId(file.getParentId())
                    .name(file.getName())
                    .path(file.getPath())
                    .fileType(file.getFileType())
                    .isDirectory(file.isDirectory())
                    .children(new ArrayList<>())
                    .build();
            nodeMap.put(file.getId(), node);

            if (file.getParentId() == null) {
                rootNodes.add(node);
            } else {
                childrenByParentId.computeIfAbsent(file.getParentId(), k -> new ArrayList<>()).add(node);
            }
        }

        // Attach children to parents
        for (Map.Entry<UUID, List<FileNodeResponse>> entry : childrenByParentId.entrySet()) {
            FileNodeResponse parentNode = nodeMap.get(entry.getKey());
            if (parentNode != null) {
                parentNode.getChildren().addAll(entry.getValue());
            }
        }

        // Sort comparator: directories first, then alphabetical by name
        Comparator<FileNodeResponse> comparator = (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        };

        // Recursively sort
        sortNodesRecursively(rootNodes, comparator);

        return rootNodes;
    }

    private void sortNodesRecursively(List<FileNodeResponse> nodes, Comparator<FileNodeResponse> comparator) {
        nodes.sort(comparator);
        for (FileNodeResponse node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortNodesRecursively(node.getChildren(), comparator);
            }
        }
    }

    public Project verifyProjectAccess(UUID projectId, UserPrincipal userPrincipal) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (userPrincipal == null) {
            return project;
        }

        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return project;
        }

        if (project.getOwnerId() != null && !project.getOwnerId().equals(userPrincipal.getId())) {
            log.warn("Access denied: User ID {} is not the owner of project ID {}", userPrincipal.getId(), projectId);
            throw new ForbiddenException("You do not have permission to access files in this project");
        }

        return project;
    }

    private ProjectFile findFileOrThrow(UUID projectId, UUID fileId) {
        return projectFileRepository.findByProjectIdAndId(projectId, fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));
    }

    private String validateAndSanitizeName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new BadRequestException("File or folder name cannot be empty");
        }
        String name = rawName.trim();
        if (name.contains("/") || name.contains("\\")) {
            throw new BadRequestException("Name cannot contain path separators ('/' or '\\')");
        }
        if (name.contains("..")) {
            throw new BadRequestException("Name cannot contain path traversal sequences ('..')");
        }
        if (name.contains("\0")) {
            throw new BadRequestException("Name cannot contain null bytes");
        }
        if (name.length() > 120) {
            throw new BadRequestException("Name cannot exceed 120 characters");
        }
        return name;
    }

    private void validatePathSafety(String path) {
        if (path == null || path.isBlank()) {
            throw new BadRequestException("Path cannot be empty");
        }
        if (path.contains("..") || path.startsWith("/") || path.startsWith("\\") || path.contains("\0") || path.matches("^[a-zA-Z]:.*")) {
            throw new BadRequestException("Invalid or unsafe path traversal detected: " + path);
        }
    }

    private void validateContentSize(String content) {
        if (content != null && content.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File content exceeds maximum allowed size of 1MB");
        }
    }

    public String determineFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "plaintext";
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "js", "mjs", "cjs" -> "javascript";
            case "jsx" -> "jsx";
            case "ts" -> "typescript";
            case "tsx" -> "tsx";
            case "html", "htm" -> "html";
            case "css" -> "css";
            case "json" -> "json";
            case "java" -> "java";
            case "py" -> "python";
            case "md", "markdown" -> "markdown";
            case "xml", "svg" -> "xml";
            case "yml", "yaml" -> "yaml";
            case "sql" -> "sql";
            case "sh", "bash" -> "shell";
            case "txt", "log" -> "plaintext";
            default -> ext;
        };
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
                .content(file.getContent())
                .size(size)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
