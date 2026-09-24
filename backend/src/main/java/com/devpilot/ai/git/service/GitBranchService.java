package com.devpilot.ai.git.service;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.git.dto.GitBranchDto;
import com.devpilot.ai.git.dto.GitBranchListResponse;
import com.devpilot.ai.git.dto.GitCheckoutRequest;
import com.devpilot.ai.git.dto.GitCreateBranchRequest;
import com.devpilot.ai.git.entity.enums.GitOperationType;
import com.devpilot.ai.git.exception.GitConflictException;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ProjectService;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class GitBranchService {

    private static final Logger log = LoggerFactory.getLogger(GitBranchService.class);
    private static final Pattern VALID_BRANCH_NAME = Pattern.compile("^[a-zA-Z0-9_.-]+(?:/[a-zA-Z0-9_.-]+)*$");

    private final GitWorkspaceManager gitWorkspaceManager;
    private final GitService gitService;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;

    public GitBranchService(GitWorkspaceManager gitWorkspaceManager,
                            GitService gitService,
                            ProjectService projectService,
                            ProjectRepository projectRepository) {
        this.gitWorkspaceManager = gitWorkspaceManager;
        this.gitService = gitService;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
    }

    /**
     * Retrieves all branches (local and remote) along with the active branch name.
     */
    public GitBranchListResponse getBranches(UUID projectId, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Repository repo = git.getRepository();
            String currentBranch = repo.getBranch();
            if (currentBranch == null || currentBranch.isBlank()) {
                currentBranch = "main";
            }

            List<GitBranchDto> branches = new ArrayList<>();

            // Local branches
            List<Ref> localRefs = git.branchList().call();
            for (Ref ref : localRefs) {
                String shortName = Repository.shortenRefName(ref.getName());
                boolean isCurrent = shortName.equals(currentBranch);
                String hash = ref.getObjectId() != null ? ref.getObjectId().getName() : "";
                branches.add(new GitBranchDto(shortName, isCurrent, false, hash));
            }

            // Remote branches
            List<Ref> remoteRefs = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
            for (Ref ref : remoteRefs) {
                String shortName = Repository.shortenRefName(ref.getName());
                String hash = ref.getObjectId() != null ? ref.getObjectId().getName() : "";
                branches.add(new GitBranchDto(shortName, false, true, hash));
            }

            return new GitBranchListResponse(currentBranch, branches);
        } catch (Exception e) {
            log.error("Failed to list branches for project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to list branches: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new Git branch.
     */
    @Transactional
    public GitBranchDto createBranch(UUID projectId, GitCreateBranchRequest request, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new GitException("Branch name cannot be empty");
        }
        String branchName = request.name().trim();
        validateBranchName(branchName);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Ref ref = git.branchCreate().setName(branchName).call();
            String hash = ref.getObjectId() != null ? ref.getObjectId().getName() : "";

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.BRANCH_CREATE, branchName, hash, "SUCCESS", "Created branch " + branchName);

            log.info("Created branch '{}' for project ID: {}", branchName, projectId);
            return new GitBranchDto(branchName, false, false, hash);
        } catch (Exception e) {
            log.error("Failed to create branch '{}' in project {}: {}", branchName, projectId, e.getMessage());
            throw new GitException("Failed to create branch: " + e.getMessage(), e);
        }
    }

    /**
     * Switches to an existing branch or creates and switches to a new branch.
     * Throws 409 GitConflictException if uncommitted changes would be overwritten.
     */
    @Transactional
    public GitBranchDto checkoutBranch(UUID projectId, GitCheckoutRequest request, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        if (request == null || request.branch() == null || request.branch().isBlank()) {
            throw new GitException("Target branch name cannot be empty");
        }
        String targetBranch = request.branch().trim();
        validateBranchName(targetBranch);

        // Sync DB files to disk first to ensure accurate dirty check
        gitWorkspaceManager.syncDbFilesToDisk(projectId);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Repository repo = git.getRepository();
            String currentBranch = repo.getBranch();

            if (targetBranch.equals(currentBranch)) {
                return new GitBranchDto(currentBranch, true, false, repo.resolve("HEAD") != null ? repo.resolve("HEAD").getName() : "");
            }

            // Check if working directory is dirty
            Status status = git.status().call();
            if (!status.isClean()) {
                List<String> uncommitted = new ArrayList<>();
                uncommitted.addAll(status.getModified());
                uncommitted.addAll(status.getAdded());
                uncommitted.addAll(status.getChanged());
                uncommitted.addAll(status.getRemoved());
                if (!uncommitted.isEmpty()) {
                    throw new GitConflictException(
                            "Cannot switch branch: you have uncommitted changes in " + uncommitted.size() + " file(s). Commit or stash your changes before switching branches.",
                            "BRANCH_SWITCH_CONFLICT",
                            uncommitted
                    );
                }
            }

            var checkoutCmd = git.checkout().setName(targetBranch);
            if (request.createIfNotExists()) {
                checkoutCmd.setCreateBranch(true);
            }
            Ref ref = checkoutCmd.call();
            String hash = ref != null && ref.getObjectId() != null ? ref.getObjectId().getName() : "";

            // Synchronize checked out files from disk back into database ProjectFiles
            gitWorkspaceManager.syncDiskFilesToDb(projectId, project);

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.BRANCH_SWITCH, targetBranch, hash, "SUCCESS", "Switched from " + currentBranch + " to " + targetBranch);

            log.info("Checked out branch '{}' in project ID: {}", targetBranch, projectId);
            return new GitBranchDto(targetBranch, true, false, hash);
        } catch (GitConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to checkout branch '{}' in project {}: {}", targetBranch, projectId, e.getMessage());
            throw new GitException("Failed to checkout branch: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a local branch. Protected against deleting default and current branch.
     */
    @Transactional
    public void deleteBranch(UUID projectId, String branchName, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        if (branchName == null || branchName.isBlank()) {
            throw new GitException("Branch name to delete cannot be empty");
        }
        String cleanBranch = branchName.trim();

        if (cleanBranch.equalsIgnoreCase("main") || cleanBranch.equalsIgnoreCase("master")) {
            throw new GitException("Cannot delete protected default branch: " + cleanBranch);
        }

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            String currentBranch = git.getRepository().getBranch();
            if (cleanBranch.equals(currentBranch)) {
                throw new GitException("Cannot delete the currently active branch '" + cleanBranch + "'. Switch to another branch first.");
            }

            git.branchDelete().setBranchNames(cleanBranch).setForce(true).call();

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.BRANCH_DELETE, cleanBranch, null, "SUCCESS", "Deleted branch " + cleanBranch);

            log.info("Deleted branch '{}' in project ID: {}", cleanBranch, projectId);
        } catch (GitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete branch '{}' in project {}: {}", cleanBranch, projectId, e.getMessage());
            throw new GitException("Failed to delete branch: " + e.getMessage(), e);
        }
    }

    private void validateBranchName(String name) {
        if (!VALID_BRANCH_NAME.matcher(name).matches() || name.contains("..") || name.endsWith("/")) {
            throw new GitException("Invalid Git branch name: '" + name + "'. Branch names must contain only alphanumeric characters, dashes, underscores, and forward slashes.");
        }
    }
}
