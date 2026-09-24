package com.devpilot.ai.git;

import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.git.service.GitWorkspaceManager;
import com.devpilot.ai.repository.ProjectFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GitWorkspaceManagerTest {

    @TempDir
    Path tempDir;

    private GitWorkspaceManager workspaceManager;
    private ProjectFileRepository projectFileRepository;

    @BeforeEach
    void setUp() {
        projectFileRepository = Mockito.mock(ProjectFileRepository.class);
        workspaceManager = new GitWorkspaceManager(
                tempDir.toString(),
                500,
                10000,
                25,
                projectFileRepository
        );
    }

    @Test
    @DisplayName("Should resolve project directory within workspace root")
    void testGetProjectDirectory() {
        UUID projectId = UUID.randomUUID();
        File dir = workspaceManager.getProjectDirectory(projectId);

        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.getAbsolutePath().startsWith(tempDir.toFile().getAbsolutePath()));
    }

    @Test
    @DisplayName("Should resolve valid relative path inside project directory")
    void testResolveValidPath() {
        UUID projectId = UUID.randomUUID();
        File resolved = workspaceManager.resolveAndValidatePath(projectId, "src/main/App.java");

        assertNotNull(resolved);
        assertTrue(resolved.getAbsolutePath().contains("src"));
        assertTrue(resolved.getAbsolutePath().endsWith("App.java"));
    }

    @Test
    @DisplayName("Should throw GitException on path traversal attempt")
    void testPathTraversalRejected() {
        UUID projectId = UUID.randomUUID();
        assertThrows(GitException.class, () ->
                workspaceManager.resolveAndValidatePath(projectId, "../../etc/passwd"));
        assertThrows(GitException.class, () ->
                workspaceManager.resolveAndValidatePath(projectId, "../outside.txt"));
    }
}
