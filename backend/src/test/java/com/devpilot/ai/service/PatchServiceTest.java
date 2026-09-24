package com.devpilot.ai.service;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.entity.ChangeSet;
import com.devpilot.ai.entity.FileChange;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.ChangeSetStatus;
import com.devpilot.ai.entity.enums.FileChangeOperation;
import com.devpilot.ai.entity.enums.FileChangeStatus;
import com.devpilot.ai.patch.*;
import com.devpilot.ai.repository.ChangeSetRepository;
import com.devpilot.ai.repository.FileChangeRepository;
import com.devpilot.ai.repository.ProjectFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatchServiceTest {

    @Mock
    private ChangeSetRepository changeSetRepository;

    @Mock
    private FileChangeRepository fileChangeRepository;

    @Mock
    private ProjectFileRepository projectFileRepository;

    @Mock
    private ProjectFileService projectFileService;

    @Mock
    private FileVersionService fileVersionService;

    @Mock
    private ActivityLogService activityLogService;

    private PatchService patchService;
    private PatchValidator patchValidator;
    private PatchDiffService patchDiffService;
    private PatchApplier patchApplier;
    private AIProperties aiProperties;

    @BeforeEach
    void setUp() {
        aiProperties = new AIProperties();
        patchValidator = new PatchValidator(aiProperties);
        patchDiffService = new PatchDiffService();
        patchApplier = new PatchApplier();

        patchService = new PatchService(
                changeSetRepository,
                fileChangeRepository,
                projectFileRepository,
                projectFileService,
                fileVersionService,
                patchValidator,
                patchDiffService,
                patchApplier,
                activityLogService,
                aiProperties
        );
    }

    @Test
    @DisplayName("Should successfully propose changeset with computed diff")
    void testProposeChangeSet() {
        UUID projectId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        ProjectFile existingFile = ProjectFile.builder()
                .id(fileId)
                .projectId(projectId)
                .path("src/App.js")
                .name("App.js")
                .content("function App() { return 1; }")
                .isDirectory(false)
                .build();

        when(projectFileRepository.findByProjectId(projectId)).thenReturn(List.of(existingFile));
        when(changeSetRepository.save(any(ChangeSet.class))).thenAnswer(inv -> {
            ChangeSet cs = inv.getArgument(0);
            cs.setId(UUID.randomUUID());
            return cs;
        });

        ChangeSetProposal proposal = new ChangeSetProposal(
                null,
                "Update App return value",
                List.of(new FileChangeProposal(
                        FileChangeOperation.UPDATE,
                        "src/App.js",
                        "function App() { return 2; }",
                        "improve"
                ))
        );

        ChangeSetResponse response = patchService.proposeChangeSet(projectId, proposal, null);

        assertNotNull(response);
        assertEquals("PROPOSED", response.getStatus());
        assertEquals(1, response.getFiles().size());
        assertTrue(response.getFiles().get(0).getDiff().contains("+"));
    }

    @Test
    @DisplayName("Should throw PatchConflictException if file was modified after proposal (optimistic lock)")
    void testApplyChangeSetConcurrencyConflict() {
        UUID projectId = UUID.randomUUID();
        UUID changeSetId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        String originalHash = HashUtils.sha256("original content");

        ChangeSet cs = new ChangeSet(changeSetId, projectId, UUID.randomUUID(), null, "Test update", ChangeSetStatus.PROPOSED);
        FileChange fc = new FileChange(
                UUID.randomUUID(),
                cs,
                fileId,
                FileChangeOperation.UPDATE,
                "src/App.js",
                "src/App.js",
                originalHash,
                HashUtils.sha256("ai proposed"),
                "original content",
                "ai proposed",
                "unified diff",
                FileChangeStatus.PROPOSED
        );
        cs.setFileChanges(List.of(fc));

        when(changeSetRepository.findByProjectIdAndId(projectId, changeSetId)).thenReturn(Optional.of(cs));

        // The file in the repository was concurrently edited by another user
        ProjectFile modifiedFile = ProjectFile.builder()
                .id(fileId)
                .projectId(projectId)
                .path("src/App.js")
                .content("concurrent user modification")
                .build();

        when(projectFileRepository.findById(fileId)).thenReturn(Optional.of(modifiedFile));

        PatchConflictException ex = assertThrows(PatchConflictException.class, () ->
                patchService.applyChangeSet(projectId, changeSetId, new ApplyChangeSetRequest(), null)
        );

        assertNotNull(ex.getConflict());
        assertEquals(fileId, ex.getConflict().getFileId());
        assertEquals(originalHash, ex.getConflict().getExpectedHash());
        assertEquals(HashUtils.sha256("concurrent user modification"), ex.getConflict().getActualHash());
    }

    @Test
    @DisplayName("Should apply changeset atomically when hashes match")
    void testApplyChangeSetSuccess() {
        UUID projectId = UUID.randomUUID();
        UUID changeSetId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        String originalContent = "console.log('hello');";
        String originalHash = HashUtils.sha256(originalContent);
        String proposedContent = "console.log('hello world');";

        ChangeSet cs = new ChangeSet(changeSetId, projectId, UUID.randomUUID(), null, "Greeting update", ChangeSetStatus.PROPOSED);
        FileChange fc = new FileChange(
                UUID.randomUUID(),
                cs,
                fileId,
                FileChangeOperation.UPDATE,
                "src/index.js",
                "src/index.js",
                originalHash,
                HashUtils.sha256(proposedContent),
                originalContent,
                proposedContent,
                "diff",
                FileChangeStatus.PROPOSED
        );
        cs.setFileChanges(List.of(fc));

        when(changeSetRepository.findByProjectIdAndId(projectId, changeSetId)).thenReturn(Optional.of(cs));

        ProjectFile currentFile = ProjectFile.builder()
                .id(fileId)
                .projectId(projectId)
                .path("src/index.js")
                .content(originalContent)
                .build();

        when(projectFileRepository.findById(fileId)).thenReturn(Optional.of(currentFile));
        when(projectFileRepository.save(any(ProjectFile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(changeSetRepository.save(any(ChangeSet.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangeSetResponse response = patchService.applyChangeSet(projectId, changeSetId, new ApplyChangeSetRequest(), null);

        assertNotNull(response);
        assertEquals("APPLIED", response.getStatus());
        assertEquals(proposedContent, currentFile.getContent());
        verify(fileVersionService).createSnapshot(currentFile, com.devpilot.ai.entity.enums.FileVersionSource.AI, null);
    }
}
