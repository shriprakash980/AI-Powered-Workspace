package com.devpilot.ai.git;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.exception.GitConflictException;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.git.repository.GitHubConnectionRepository;
import com.devpilot.ai.git.repository.GitOperationRepository;
import com.devpilot.ai.git.security.TokenEncryptionService;
import com.devpilot.ai.git.service.*;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GitCoreServiceTest {

    @TempDir
    Path tempDir;

    private GitWorkspaceManager workspaceManager;
    private GitService gitService;
    private GitStatusService gitStatusService;
    private GitCommitService gitCommitService;
    private GitBranchService gitBranchService;
    private GitRemoteService gitRemoteService;

    private ProjectRepository projectRepository;
    private ProjectFileRepository projectFileRepository;
    private ProjectService projectService;
    private GitOperationRepository gitOperationRepository;
    private GitHubConnectionRepository gitHubConnectionRepository;
    private TokenEncryptionService tokenEncryptionService;

    private UUID projectId;
    private UserPrincipal testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        testUser = new UserPrincipal(
                userId,
                "Test User",
                "test@devpilot.ai",
                "hashedpassword",
                com.devpilot.ai.entity.enums.UserStatus.ACTIVE,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        projectRepository = Mockito.mock(ProjectRepository.class);
        projectFileRepository = Mockito.mock(ProjectFileRepository.class);
        projectService = Mockito.mock(ProjectService.class);
        gitOperationRepository = Mockito.mock(GitOperationRepository.class);
        gitHubConnectionRepository = Mockito.mock(GitHubConnectionRepository.class);
        tokenEncryptionService = new TokenEncryptionService("devpilot-test-key-32chars-min-aes256");

        testProject = Project.builder()
                .id(projectId)
                .name("Git Test Project")
                .ownerId(userId)
                .gitEnabled(false)
                .defaultBranch("main")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workspaceManager = new GitWorkspaceManager(
                tempDir.toString(),
                500,
                10000,
                25,
                projectFileRepository
        );

        gitService = new GitService(projectRepository, projectService, workspaceManager, gitOperationRepository);
        gitStatusService = new GitStatusService(workspaceManager, projectService);
        gitCommitService = new GitCommitService(workspaceManager, gitService, projectService);
        gitBranchService = new GitBranchService(workspaceManager, gitService, projectService, projectRepository);
        gitRemoteService = new GitRemoteService(workspaceManager, gitService, projectService, projectRepository, gitHubConnectionRepository, tokenEncryptionService);
    }

    @Test
    @DisplayName("Should successfully initialize Git repository and report clean status")
    void testInitAndStatus() {
        GitInitResponse initResp = gitService.initRepository(projectId, testUser);
        assertNotNull(initResp);
        assertTrue(initResp.initialized());
        assertEquals("main", initResp.defaultBranch());

        GitStatusResponse status = gitStatusService.getStatus(projectId, testUser);
        assertNotNull(status);
        assertEquals("main", status.branch());
        assertTrue(status.clean());
    }

    @Test
    @DisplayName("Should detect modified file, stage it, and commit with message")
    void testStageAndCommitFlow() throws IOException {
        gitService.initRepository(projectId, testUser);

        // Create a test file in the workspace
        File projDir = workspaceManager.getProjectDirectory(projectId);
        File testFile = new File(projDir, "README.md");
        Files.writeString(testFile.toPath(), "# DevPilot Workspace");

        // Verify status shows untracked file
        GitStatusResponse status1 = gitStatusService.getStatus(projectId, testUser);
        assertFalse(status1.clean());
        assertEquals(1, status1.files().size());
        assertEquals("UNTRACKED", status1.files().get(0).status());

        // Stage file
        gitCommitService.stage(projectId, List.of("README.md"), testUser);

        GitStatusResponse status2 = gitStatusService.getStatus(projectId, testUser);
        assertEquals(1, status2.stagedCount());

        // Commit changes
        GitCommitResponse commitResp = gitCommitService.commit(
                projectId,
                new GitCommitRequest("Initial workspace commit"),
                testUser
        );

        assertNotNull(commitResp.commitHash());
        assertEquals("main", commitResp.branch());
        assertEquals("Initial workspace commit", commitResp.message());

        // Check commit history
        List<GitCommitDto> commits = gitCommitService.getCommits(projectId, "main", 0, 10, null, testUser);
        assertEquals(1, commits.size());
        assertEquals(commitResp.commitHash(), commits.get(0).commitHash());
    }

    @Test
    @DisplayName("Should reject empty commit message or commit with no staged changes")
    void testCommitValidations() {
        gitService.initRepository(projectId, testUser);

        // Empty message
        assertThrows(GitException.class, () ->
                gitCommitService.commit(projectId, new GitCommitRequest("   "), testUser));

        // Nothing staged
        assertThrows(GitException.class, () ->
                gitCommitService.commit(projectId, new GitCommitRequest("Valid message"), testUser));
    }

    @Test
    @DisplayName("Should create, list, and switch branches")
    void testBranchWorkflow() throws IOException {
        gitService.initRepository(projectId, testUser);

        // Need at least one commit to branch cleanly in Git
        File projDir = workspaceManager.getProjectDirectory(projectId);
        Files.writeString(new File(projDir, "file.txt").toPath(), "initial content");
        gitCommitService.stageAll(projectId, testUser);
        gitCommitService.commit(projectId, new GitCommitRequest("Initial commit"), testUser);

        // Create feature branch
        GitBranchDto created = gitBranchService.createBranch(projectId, new GitCreateBranchRequest("feature/login"), testUser);
        assertNotNull(created);
        assertEquals("feature/login", created.name());

        // List branches
        GitBranchListResponse branchList = gitBranchService.getBranches(projectId, testUser);
        assertEquals("main", branchList.currentBranch());
        assertTrue(branchList.branches().stream().anyMatch(b -> b.name().equals("feature/login")));

        // Checkout feature branch
        GitBranchDto checkedOut = gitBranchService.checkoutBranch(
                projectId,
                new GitCheckoutRequest("feature/login", false),
                testUser
        );
        assertEquals("feature/login", checkedOut.name());
        assertTrue(checkedOut.current());
    }

    @Test
    @DisplayName("Should prevent deleting active or default protected branch")
    void testBranchDeletionRules() throws IOException {
        gitService.initRepository(projectId, testUser);

        File projDir = workspaceManager.getProjectDirectory(projectId);
        Files.writeString(new File(projDir, "file.txt").toPath(), "hello");
        gitCommitService.stageAll(projectId, testUser);
        gitCommitService.commit(projectId, new GitCommitRequest("Initial"), testUser);

        // Cannot delete main
        assertThrows(GitException.class, () ->
                gitBranchService.deleteBranch(projectId, "main", testUser));

        // Cannot delete current active branch
        assertThrows(GitException.class, () ->
                gitBranchService.deleteBranch(projectId, "main", testUser));
    }

    @Test
    @DisplayName("Should reject illegal remote URLs")
    void testRemoteValidation() {
        gitService.initRepository(projectId, testUser);

        assertThrows(GitException.class, () ->
                gitRemoteService.addRemote(projectId, new GitAddRemoteRequest("origin", "file:///etc/repo.git"), testUser));

        assertThrows(GitException.class, () ->
                gitRemoteService.addRemote(projectId, new GitAddRemoteRequest("origin", "C:/repo.git"), testUser));
    }
}
