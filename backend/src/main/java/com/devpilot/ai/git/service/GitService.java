package com.devpilot.ai.git.service;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.git.dto.GitInitResponse;
import com.devpilot.ai.git.entity.GitOperation;
import com.devpilot.ai.git.entity.enums.GitOperationType;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.git.repository.GitOperationRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ProjectService;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.UUID;

@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final GitWorkspaceManager gitWorkspaceManager;
    private final GitOperationRepository gitOperationRepository;

    public GitService(ProjectRepository projectRepository,
                      ProjectService projectService,
                      GitWorkspaceManager gitWorkspaceManager,
                      GitOperationRepository gitOperationRepository) {
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.gitWorkspaceManager = gitWorkspaceManager;
        this.gitOperationRepository = gitOperationRepository;
    }

    /**
     * Initializes a Git repository for the project workspace.
     */
    @Transactional
    public GitInitResponse initRepository(UUID projectId, UserPrincipal userPrincipal) {
        log.info("Initializing Git repository for project ID: {}", projectId);
        projectService.getProjectById(projectId, userPrincipal); // verifies ownership

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        File projectDir = gitWorkspaceManager.getProjectDirectory(projectId);
        File gitDir = new File(projectDir, ".git");

        boolean alreadyInitialized = gitDir.exists() && gitDir.isDirectory();
        String defaultBranch = project.getDefaultBranch() != null ? project.getDefaultBranch() : "main";

        if (!alreadyInitialized) {
            try {
                // Sync any existing files from DB to disk
                gitWorkspaceManager.syncDbFilesToDisk(projectId);

                try (Git git = Git.init()
                        .setDirectory(projectDir)
                        .setInitialBranch(defaultBranch)
                        .call()) {
                    gitWorkspaceManager.secureGitConfig(git);

                    // Configure default committer details if not set
                    var config = git.getRepository().getConfig();
                    String userEmail = userPrincipal != null ? userPrincipal.getEmail() : "devpilot@workspace.local";
                    String userName = userPrincipal != null ? userPrincipal.getUsername() : "DevPilot User";
                    config.setString("user", null, "name", userName);
                    config.setString("user", null, "email", userEmail);
                    config.save();
                }

                project.setGitEnabled(true);
                project.setDefaultBranch(defaultBranch);
                projectRepository.save(project);

                logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                        GitOperationType.INIT, defaultBranch, null, "SUCCESS", "Git repository initialized successfully");

                log.info("Initialized new Git repository for project ID: {}", projectId);
                return new GitInitResponse(projectId, true, defaultBranch, "Git repository initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Git repository for project: {}", projectId, e);
                logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                        GitOperationType.INIT, defaultBranch, null, "FAILED", e.getMessage());
                throw new GitException("Failed to initialize Git repository: " + e.getMessage(), e);
            }
        } else {
            project.setGitEnabled(true);
            projectRepository.save(project);
            return new GitInitResponse(projectId, true, defaultBranch, "Git repository is already initialized");
        }
    }

    public void logOperation(UUID projectId, UUID userId, GitOperationType operation,
                             String branch, String commitHash, String status, String message) {
        try {
            if (projectId != null && userId != null) {
                GitOperation op = GitOperation.builder()
                        .projectId(projectId)
                        .userId(userId)
                        .operation(operation)
                        .branch(branch)
                        .commitHash(commitHash)
                        .status(status)
                        .message(message)
                        .build();
                gitOperationRepository.save(op);
            }
        } catch (Exception e) {
            log.warn("Failed to record Git operation audit log: {}", e.getMessage());
        }
    }
}
