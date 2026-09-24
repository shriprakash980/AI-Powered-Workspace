package com.devpilot.ai.service;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.entity.FileVersion;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.FileVersionSource;
import com.devpilot.ai.patch.FileVersionResponse;
import com.devpilot.ai.repository.FileVersionRepository;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileVersionServiceTest {

    @Mock
    private FileVersionRepository fileVersionRepository;

    @Mock
    private ProjectFileRepository projectFileRepository;

    @Mock
    private ProjectFileService projectFileService;

    @Mock
    private ActivityLogService activityLogService;

    private FileVersionService fileVersionService;
    private AIProperties aiProperties;

    @BeforeEach
    void setUp() {
        aiProperties = new AIProperties();
        fileVersionService = new FileVersionService(
                fileVersionRepository,
                projectFileRepository,
                projectFileService,
                activityLogService,
                aiProperties
        );
    }

    @Test
    @DisplayName("createSnapshot increments version number")
    void testCreateSnapshotIncrementsVersion() {
        UUID fileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectFile file = ProjectFile.builder()
                .id(fileId)
                .path("src/App.java")
                .content("public class App {}")
                .isDirectory(false)
                .build();

        FileVersion previousVersion = new FileVersion(UUID.randomUUID(), fileId, 2, "old", "hash", FileVersionSource.MANUAL, userId);
        when(fileVersionRepository.findTopByFileIdOrderByVersionNumberDesc(fileId))
                .thenReturn(Optional.of(previousVersion));
        when(fileVersionRepository.save(any(FileVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        FileVersion snapshot = fileVersionService.createSnapshot(file, FileVersionSource.AI, userId);

        assertNotNull(snapshot);
        assertEquals(3, snapshot.getVersionNumber());
        assertEquals(FileVersionSource.AI, snapshot.getSource());
    }

    @Test
    @DisplayName("restoreVersion restores content and creates ROLLBACK snapshot non-destructively")
    void testRestoreVersionNonDestructive() {
        UUID projectId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectFile file = ProjectFile.builder()
                .id(fileId)
                .projectId(projectId)
                .path("src/App.java")
                .content("modified content")
                .isDirectory(false)
                .build();

        FileVersion historicalVersion = new FileVersion(
                versionId,
                fileId,
                1,
                "original content v1",
                "hash1",
                FileVersionSource.MANUAL,
                userId
        );

        when(projectFileRepository.findByProjectIdAndId(projectId, fileId)).thenReturn(Optional.of(file));
        when(fileVersionRepository.findById(versionId)).thenReturn(Optional.of(historicalVersion));
        when(fileVersionRepository.findTopByFileIdOrderByVersionNumberDesc(fileId)).thenReturn(Optional.of(historicalVersion));
        when(fileVersionRepository.save(any(FileVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        FileVersionResponse response = fileVersionService.restoreVersion(projectId, fileId, versionId, null);

        assertNotNull(response);
        assertEquals("original content v1", file.getContent());
        assertEquals("ROLLBACK", response.getSource());
        verify(projectFileRepository).save(file);
    }
}
