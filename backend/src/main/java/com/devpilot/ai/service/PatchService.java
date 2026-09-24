package com.devpilot.ai.service;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.entity.ChangeSet;
import com.devpilot.ai.entity.FileChange;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.ChangeSetStatus;
import com.devpilot.ai.entity.enums.FileChangeOperation;
import com.devpilot.ai.entity.enums.FileChangeStatus;
import com.devpilot.ai.entity.enums.FileVersionSource;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.patch.*;
import com.devpilot.ai.repository.ChangeSetRepository;
import com.devpilot.ai.repository.FileChangeRepository;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PatchService {

    private static final Logger log = LoggerFactory.getLogger(PatchService.class);

    private final ChangeSetRepository changeSetRepository;
    private final FileChangeRepository fileChangeRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileService projectFileService;
    private final FileVersionService fileVersionService;
    private final PatchValidator patchValidator;
    private final PatchDiffService patchDiffService;
    private final PatchApplier patchApplier;
    private final ActivityLogService activityLogService;
    private final AIProperties aiProperties;

    public PatchService(ChangeSetRepository changeSetRepository,
                        FileChangeRepository fileChangeRepository,
                        ProjectFileRepository projectFileRepository,
                        ProjectFileService projectFileService,
                        FileVersionService fileVersionService,
                        PatchValidator patchValidator,
                        PatchDiffService patchDiffService,
                        PatchApplier patchApplier,
                        ActivityLogService activityLogService,
                        AIProperties aiProperties) {
        this.changeSetRepository = changeSetRepository;
        this.fileChangeRepository = fileChangeRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectFileService = projectFileService;
        this.fileVersionService = fileVersionService;
        this.patchValidator = patchValidator;
        this.patchDiffService = patchDiffService;
        this.patchApplier = patchApplier;
        this.activityLogService = activityLogService;
        this.aiProperties = aiProperties;
    }

    public ChangeSetResponse proposeChangeSet(UUID projectId, ChangeSetProposal proposal, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);

        List<ProjectFile> existingFiles = projectFileRepository.findByProjectId(projectId);
        PatchValidationResult validation = patchValidator.validate(proposal);
        if (!validation.isValid()) {
            throw new BadRequestException("ChangeSet proposal is invalid: " + String.join(", ", validation.getErrors()));
        }

        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        ChangeSet changeSet = new ChangeSet(
                null,
                projectId,
                userId,
                proposal.getConversationId(),
                proposal.getSummary() != null ? proposal.getSummary() : "AI Proposed Changes",
                ChangeSetStatus.PROPOSED
        );

        Map<String, ProjectFile> pathToFileMap = existingFiles.stream()
                .filter(f -> !f.isDirectory())
                .collect(Collectors.toMap(ProjectFile::getPath, f -> f, (a, b) -> a));

        List<FileChange> fileChanges = new ArrayList<>();

        for (FileChangeProposal fileProp : proposal.getChanges()) {
            String path = fileProp.getFilePath();
            ProjectFile existing = pathToFileMap.get(path);
            String originalContent = (existing != null && existing.getContent() != null) ? existing.getContent() : "";
            String oldHash = existing != null ? HashUtils.sha256(originalContent) : null;

            String proposedContent;
            if (fileProp.getOperation() == FileChangeOperation.DELETE) {
                proposedContent = "";
            } else {
                proposedContent = patchApplier.computeProposedContent(originalContent, fileProp);
            }

            String newHash = HashUtils.sha256(proposedContent);
            PatchDiffService.DiffSummary diffSummary = patchDiffService.computeDiff(originalContent, proposedContent, path);

            FileChange change = new FileChange(
                    null,
                    changeSet,
                    existing != null ? existing.getId() : null,
                    fileProp.getOperation(),
                    fileProp.getOldPath(),
                    fileProp.getNewPath(),
                    oldHash,
                    newHash,
                    originalContent,
                    proposedContent,
                    diffSummary.getUnifiedDiff(),
                    FileChangeStatus.PROPOSED
            );

            fileChanges.add(change);
        }

        changeSet.setFileChanges(fileChanges);
        ChangeSet saved = changeSetRepository.save(changeSet);

        activityLogService.logActivity(
                userId,
                projectId,
                "PROPOSE_CHANGESET",
                "Proposed changeset: " + changeSet.getSummary() + " (" + fileChanges.size() + " files)"
        );

        log.info("Created ChangeSet ID: {} with {} file changes for project: {}", saved.getId(), fileChanges.size(), projectId);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChangeSetResponse> getChangeSets(UUID projectId, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);
        return changeSetRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChangeSetResponse getChangeSet(UUID projectId, UUID changeSetId, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);
        ChangeSet cs = changeSetRepository.findByProjectIdAndId(projectId, changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("ChangeSet", "id", changeSetId));
        return mapToResponse(cs);
    }

    public ChangeSetResponse applyChangeSet(UUID projectId, UUID changeSetId, ApplyChangeSetRequest request, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);

        ChangeSet changeSet = changeSetRepository.findByProjectIdAndId(projectId, changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("ChangeSet", "id", changeSetId));

        if (changeSet.getStatus() == ChangeSetStatus.APPLIED) {
            throw new BadRequestException("ChangeSet has already been applied");
        }
        if (changeSet.getStatus() == ChangeSetStatus.REJECTED) {
            throw new BadRequestException("Cannot apply a rejected ChangeSet");
        }

        List<FileChange> allChanges = changeSet.getFileChanges();
        List<FileChange> changesToApply;

        if (request != null && request.getFileChangeIds() != null && !request.getFileChangeIds().isEmpty()) {
            Set<UUID> targetIds = new HashSet<>(request.getFileChangeIds());
            changesToApply = allChanges.stream()
                    .filter(c -> targetIds.contains(c.getId()))
                    .collect(Collectors.toList());
        } else {
            changesToApply = allChanges;
        }

        if (changesToApply.isEmpty()) {
            throw new BadRequestException("No valid file changes selected to apply");
        }

        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;

        // Step 1: Pre-flight Optimistic Concurrency check for ALL files to be applied
        for (FileChange change : changesToApply) {
            if (change.getOperation() == FileChangeOperation.UPDATE ||
                change.getOperation() == FileChangeOperation.DELETE ||
                change.getOperation() == FileChangeOperation.RENAME) {

                ProjectFile file = null;
                if (change.getFileId() != null) {
                    file = projectFileRepository.findById(change.getFileId()).orElse(null);
                }
                if (file == null && change.getOldPath() != null) {
                    file = projectFileRepository.findByProjectIdAndPath(projectId, change.getOldPath()).orElse(null);
                }

                if (file == null) {
                    PatchConflict conflict = new PatchConflict(
                            change.getFileId(),
                            change.getOldPath(),
                            change.getOldContentHash(),
                            null,
                            "Target file '" + change.getOldPath() + "' no longer exists in project."
                    );
                    change.setStatus(FileChangeStatus.CONFLICTED);
                    changeSet.setStatus(ChangeSetStatus.CONFLICTED);
                    changeSetRepository.save(changeSet);
                    throw new PatchConflictException(conflict);
                }

                String currentContent = file.getContent() != null ? file.getContent() : "";
                String currentHash = HashUtils.sha256(currentContent);

                if (change.getOldContentHash() != null && !change.getOldContentHash().equalsIgnoreCase(currentHash)) {
                    log.warn("Concurrency conflict for file {}: expected hash {} but found {}",
                            file.getPath(), change.getOldContentHash(), currentHash);
                    change.setStatus(FileChangeStatus.CONFLICTED);
                    changeSet.setStatus(ChangeSetStatus.CONFLICTED);
                    changeSetRepository.save(changeSet);
                    PatchConflict conflict = new PatchConflict(
                            file.getId(),
                            file.getPath(),
                            change.getOldContentHash(),
                            currentHash,
                            "File '" + file.getPath() + "' was modified after changeset proposal. Application aborted."
                    );
                    throw new PatchConflictException(conflict);
                }
            }
        }

        // Step 2: Atomic Execution of changes
        Instant now = Instant.now();
        for (FileChange change : changesToApply) {
            applySingleChange(projectId, change, userId, now);
        }

        boolean allApplied = allChanges.stream().allMatch(c -> c.getStatus() == FileChangeStatus.APPLIED);
        changeSet.setStatus(allApplied ? ChangeSetStatus.APPLIED : ChangeSetStatus.PARTIALLY_APPLIED);
        changeSet.setAppliedAt(now);
        ChangeSet saved = changeSetRepository.save(changeSet);

        activityLogService.logActivity(
                userId,
                projectId,
                "APPLY_CHANGESET",
                "Applied " + changesToApply.size() + " change(s) from changeset: " + changeSet.getSummary()
        );

        return mapToResponse(saved);
    }

    private void applySingleChange(UUID projectId, FileChange change, UUID userId, Instant timestamp) {
        FileChangeOperation op = change.getOperation();
        String targetPath = change.getNewPath() != null ? change.getNewPath() : change.getOldPath();

        switch (op) {
            case CREATE -> {
                UUID parentId = ensureParentDirectories(projectId, targetPath);
                String fileName = extractFileName(targetPath);
                String fileType = determineFileType(fileName);
                String content = change.getProposedContent() != null ? change.getProposedContent() : "";

                ProjectFile newFile = ProjectFile.builder()
                        .projectId(projectId)
                        .parentId(parentId)
                        .name(fileName)
                        .path(targetPath)
                        .fileType(fileType)
                        .content(content)
                        .isDirectory(false)
                        .createdAt(timestamp)
                        .updatedAt(timestamp)
                        .build();

                ProjectFile saved = projectFileRepository.save(newFile);
                change.setFileId(saved.getId());
                fileVersionService.createSnapshot(saved, FileVersionSource.AI, userId);
            }
            case UPDATE -> {
                ProjectFile file = resolveFile(projectId, change);
                // Snapshot original state if not yet snapshotted
                fileVersionService.createSnapshot(file, FileVersionSource.MANUAL, userId);

                String content = change.getProposedContent() != null ? change.getProposedContent() : "";
                file.setContent(content);
                file.setUpdatedAt(timestamp);
                ProjectFile saved = projectFileRepository.save(file);

                // Snapshot new state as AI
                fileVersionService.createSnapshot(saved, FileVersionSource.AI, userId);
            }
            case DELETE -> {
                ProjectFile file = resolveFile(projectId, change);
                fileVersionService.createSnapshot(file, FileVersionSource.MANUAL, userId);
                projectFileRepository.delete(file);
            }
            case RENAME -> {
                ProjectFile file = resolveFile(projectId, change);
                fileVersionService.createSnapshot(file, FileVersionSource.MANUAL, userId);

                String newPath = change.getNewPath();
                UUID parentId = ensureParentDirectories(projectId, newPath);
                file.setPath(newPath);
                file.setName(extractFileName(newPath));
                file.setParentId(parentId);
                file.setUpdatedAt(timestamp);
                ProjectFile saved = projectFileRepository.save(file);

                fileVersionService.createSnapshot(saved, FileVersionSource.AI, userId);
            }
        }

        change.setStatus(FileChangeStatus.APPLIED);
        change.setAppliedAt(timestamp);
    }

    public ChangeSetResponse rejectChangeSet(UUID projectId, UUID changeSetId, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);

        ChangeSet changeSet = changeSetRepository.findByProjectIdAndId(projectId, changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("ChangeSet", "id", changeSetId));

        if (changeSet.getStatus() == ChangeSetStatus.APPLIED) {
            throw new BadRequestException("Cannot reject an already applied ChangeSet. Use rollback instead.");
        }

        Instant now = Instant.now();
        changeSet.setStatus(ChangeSetStatus.REJECTED);
        changeSet.setRejectedAt(now);

        for (FileChange fc : changeSet.getFileChanges()) {
            if (fc.getStatus() == FileChangeStatus.PROPOSED) {
                fc.setStatus(FileChangeStatus.REJECTED);
            }
        }

        ChangeSet saved = changeSetRepository.save(changeSet);
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        activityLogService.logActivity(
                userId,
                projectId,
                "REJECT_CHANGESET",
                "Rejected changeset: " + changeSet.getSummary()
        );

        return mapToResponse(saved);
    }

    public ChangeSetResponse rollbackChangeSet(UUID projectId, UUID changeSetId, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);

        ChangeSet changeSet = changeSetRepository.findByProjectIdAndId(projectId, changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("ChangeSet", "id", changeSetId));

        if (changeSet.getStatus() != ChangeSetStatus.APPLIED && changeSet.getStatus() != ChangeSetStatus.PARTIALLY_APPLIED) {
            throw new BadRequestException("Only applied or partially applied ChangeSets can be rolled back.");
        }

        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        Instant now = Instant.now();

        for (FileChange fc : changeSet.getFileChanges()) {
            if (fc.getStatus() == FileChangeStatus.APPLIED) {
                rollbackSingleChange(projectId, fc, userId, now);
            }
        }

        changeSet.setStatus(ChangeSetStatus.ROLLED_BACK);
        ChangeSet saved = changeSetRepository.save(changeSet);

        activityLogService.logActivity(
                userId,
                projectId,
                "ROLLBACK_CHANGESET",
                "Rolled back changeset: " + changeSet.getSummary()
        );

        return mapToResponse(saved);
    }

    private void rollbackSingleChange(UUID projectId, FileChange change, UUID userId, Instant timestamp) {
        FileChangeOperation op = change.getOperation();
        switch (op) {
            case CREATE -> {
                // Delete the created file
                ProjectFile file = resolveFile(projectId, change);
                if (file != null) {
                    projectFileRepository.delete(file);
                }
            }
            case UPDATE -> {
                // Revert to original content
                ProjectFile file = resolveFile(projectId, change);
                if (file != null) {
                    String original = change.getOriginalContent() != null ? change.getOriginalContent() : "";
                    file.setContent(original);
                    file.setUpdatedAt(timestamp);
                    ProjectFile saved = projectFileRepository.save(file);
                    fileVersionService.createSnapshot(saved, FileVersionSource.ROLLBACK, userId);
                }
            }
            case DELETE -> {
                // Recreate the deleted file
                String path = change.getOldPath();
                UUID parentId = ensureParentDirectories(projectId, path);
                String fileName = extractFileName(path);
                String fileType = determineFileType(fileName);
                String original = change.getOriginalContent() != null ? change.getOriginalContent() : "";

                ProjectFile restored = ProjectFile.builder()
                        .projectId(projectId)
                        .parentId(parentId)
                        .name(fileName)
                        .path(path)
                        .fileType(fileType)
                        .content(original)
                        .isDirectory(false)
                        .createdAt(timestamp)
                        .updatedAt(timestamp)
                        .build();

                ProjectFile saved = projectFileRepository.save(restored);
                fileVersionService.createSnapshot(saved, FileVersionSource.ROLLBACK, userId);
            }
            case RENAME -> {
                // Revert path
                ProjectFile file = resolveFile(projectId, change);
                if (file != null) {
                    String oldPath = change.getOldPath();
                    UUID parentId = ensureParentDirectories(projectId, oldPath);
                    file.setPath(oldPath);
                    file.setName(extractFileName(oldPath));
                    file.setParentId(parentId);
                    file.setUpdatedAt(timestamp);
                    ProjectFile saved = projectFileRepository.save(file);
                    fileVersionService.createSnapshot(saved, FileVersionSource.ROLLBACK, userId);
                }
            }
        }
    }

    private ProjectFile resolveFile(UUID projectId, FileChange change) {
        if (change.getFileId() != null) {
            Optional<ProjectFile> byId = projectFileRepository.findById(change.getFileId());
            if (byId.isPresent()) return byId.get();
        }
        String path = change.getOldPath() != null ? change.getOldPath() : change.getNewPath();
        return projectFileRepository.findByProjectIdAndPath(projectId, path)
                .orElseThrow(() -> new ResourceNotFoundException("File", "path", path));
    }

    private UUID ensureParentDirectories(UUID projectId, String filePath) {
        if (!filePath.contains("/")) {
            return null;
        }
        String[] parts = filePath.split("/");
        StringBuilder currentPath = new StringBuilder();
        UUID currentParentId = null;

        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) currentPath.append("/");
            currentPath.append(parts[i]);
            String folderPath = currentPath.toString();

            Optional<ProjectFile> existingFolder = projectFileRepository.findByProjectIdAndPath(projectId, folderPath);
            if (existingFolder.isPresent()) {
                currentParentId = existingFolder.get().getId();
            } else {
                ProjectFile folder = ProjectFile.builder()
                        .projectId(projectId)
                        .parentId(currentParentId)
                        .name(parts[i])
                        .path(folderPath)
                        .fileType("directory")
                        .content(null)
                        .isDirectory(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                ProjectFile savedFolder = projectFileRepository.save(folder);
                currentParentId = savedFolder.getId();
            }
        }
        return currentParentId;
    }

    private String extractFileName(String path) {
        if (path == null) return "unknown";
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }

    private String determineFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "text";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "java" -> "java";
            case "py" -> "python";
            case "html" -> "html";
            case "css" -> "css";
            case "json" -> "json";
            case "md" -> "markdown";
            case "sql" -> "sql";
            default -> "text";
        };
    }

    public ChangeSetResponse mapToResponse(ChangeSet cs) {
        List<FileChangeResponse> files = cs.getFileChanges().stream()
                .map(this::mapToFileChangeResponse)
                .collect(Collectors.toList());

        return new ChangeSetResponse(
                cs.getId(),
                cs.getProjectId(),
                cs.getUserId(),
                cs.getConversationId(),
                cs.getSummary(),
                cs.getStatus() != null ? cs.getStatus().name() : ChangeSetStatus.PROPOSED.name(),
                files,
                cs.getCreatedAt(),
                cs.getAppliedAt(),
                cs.getRejectedAt()
        );
    }

    public FileChangeResponse mapToFileChangeResponse(FileChange fc) {
        int additions = 0;
        int deletions = 0;
        if (fc.getDiff() != null) {
            String[] lines = fc.getDiff().split("\n");
            for (String l : lines) {
                if (l.startsWith("+") && !l.startsWith("+++")) additions++;
                else if (l.startsWith("-") && !l.startsWith("---")) deletions++;
            }
        }

        return new FileChangeResponse(
                fc.getId(),
                fc.getFileId(),
                fc.getOperation() != null ? fc.getOperation().name() : FileChangeOperation.UPDATE.name(),
                fc.getOldPath(),
                fc.getNewPath(),
                fc.getOldContentHash(),
                fc.getProposedContentHash(),
                fc.getOriginalContent(),
                fc.getProposedContent(),
                fc.getDiff(),
                fc.getStatus() != null ? fc.getStatus().name() : FileChangeStatus.PROPOSED.name(),
                additions,
                deletions,
                fc.getCreatedAt(),
                fc.getAppliedAt()
        );
    }
}
