package com.devpilot.ai.service;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.entity.FileVersion;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.FileVersionSource;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.patch.FileVersionResponse;
import com.devpilot.ai.patch.HashUtils;
import com.devpilot.ai.repository.FileVersionRepository;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FileVersionService {

    private static final Logger log = LoggerFactory.getLogger(FileVersionService.class);

    private final FileVersionRepository fileVersionRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileService projectFileService;
    private final ActivityLogService activityLogService;
    private final AIProperties aiProperties;

    public FileVersionService(FileVersionRepository fileVersionRepository,
                              ProjectFileRepository projectFileRepository,
                              ProjectFileService projectFileService,
                              ActivityLogService activityLogService,
                              AIProperties aiProperties) {
        this.fileVersionRepository = fileVersionRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectFileService = projectFileService;
        this.activityLogService = activityLogService;
        this.aiProperties = aiProperties;
    }

    public FileVersion createSnapshot(ProjectFile file, FileVersionSource source, UUID userId) {
        if (file == null || file.isDirectory()) {
            return null;
        }

        String content = file.getContent() != null ? file.getContent() : "";
        if (content.length() > aiProperties.getVersion().getMaxContentSize()) {
            log.warn("File {} content length {} exceeds max version snapshot limit {}",
                    file.getPath(), content.length(), aiProperties.getVersion().getMaxContentSize());
        }

        String hash = HashUtils.sha256(content);

        int nextVersion = fileVersionRepository.findTopByFileIdOrderByVersionNumberDesc(file.getId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        FileVersion fileVersion = new FileVersion(
                null,
                file.getId(),
                nextVersion,
                content,
                hash,
                source != null ? source : FileVersionSource.MANUAL,
                userId
        );

        FileVersion saved = fileVersionRepository.save(fileVersion);
        log.info("Created file version v{} (id: {}) for file: {}", nextVersion, saved.getId(), file.getPath());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FileVersionResponse> getVersions(UUID projectId, UUID fileId, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);
        verifyFileInProject(projectId, fileId);

        return fileVersionRepository.findByFileIdOrderByVersionNumberDesc(fileId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FileVersionResponse getVersion(UUID projectId, UUID fileId, UUID versionId, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);
        verifyFileInProject(projectId, fileId);

        FileVersion version = fileVersionRepository.findById(versionId)
                .filter(v -> v.getFileId().equals(fileId))
                .orElseThrow(() -> new ResourceNotFoundException("FileVersion", "id", versionId));

        return mapToResponse(version);
    }

    public FileVersionResponse restoreVersion(UUID projectId, UUID fileId, UUID versionId, UserPrincipal userPrincipal) {
        projectFileService.verifyProjectAccess(projectId, userPrincipal);
        ProjectFile file = verifyFileInProject(projectId, fileId);

        if (file.isDirectory()) {
            throw new BadRequestException("Cannot restore version for a directory");
        }

        FileVersion targetVersion = fileVersionRepository.findById(versionId)
                .filter(v -> v.getFileId().equals(fileId))
                .orElseThrow(() -> new ResourceNotFoundException("FileVersion", "id", versionId));

        // Restore file content
        String restoredContent = targetVersion.getContent() != null ? targetVersion.getContent() : "";
        file.setContent(restoredContent);
        projectFileRepository.save(file);

        // Snapshot new version with ROLLBACK source
        UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
        FileVersion rollbackVersion = createSnapshot(file, FileVersionSource.ROLLBACK, userId);

        activityLogService.logActivity(
                userId,
                projectId,
                "RESTORE_FILE_VERSION",
                "Restored file " + file.getPath() + " to version v" + targetVersion.getVersionNumber()
        );

        log.info("Restored file {} to v{} with new rollback snapshot v{}",
                file.getPath(), targetVersion.getVersionNumber(), rollbackVersion.getVersionNumber());

        return mapToResponse(rollbackVersion);
    }

    private ProjectFile verifyFileInProject(UUID projectId, UUID fileId) {
        return projectFileRepository.findByProjectIdAndId(projectId, fileId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectFile", "id", fileId));
    }

    public FileVersionResponse mapToResponse(FileVersion v) {
        return new FileVersionResponse(
                v.getId(),
                v.getFileId(),
                v.getVersionNumber(),
                v.getContent(),
                v.getContentHash(),
                v.getSource() != null ? v.getSource().name() : FileVersionSource.MANUAL.name(),
                v.getCreatedBy(),
                v.getCreatedAt()
        );
    }
}
