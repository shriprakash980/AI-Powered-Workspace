package com.devpilot.ai.git.service;

import com.devpilot.ai.git.dto.GitFileStatusDto;
import com.devpilot.ai.git.dto.GitStatusResponse;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ProjectService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GitStatusService {

    private static final Logger log = LoggerFactory.getLogger(GitStatusService.class);

    private final GitWorkspaceManager gitWorkspaceManager;
    private final ProjectService projectService;

    public GitStatusService(GitWorkspaceManager gitWorkspaceManager, ProjectService projectService) {
        this.gitWorkspaceManager = gitWorkspaceManager;
        this.projectService = projectService;
    }

    public GitStatusResponse getStatus(UUID projectId, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal); // verify ownership

        // Sync latest database changes into the disk workspace
        gitWorkspaceManager.syncDbFilesToDisk(projectId);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Repository repository = git.getRepository();
            String currentBranch = repository.getBranch();
            if (currentBranch == null || currentBranch.isBlank()) {
                currentBranch = "main";
            }

            Status status = git.status().call();

            Map<String, GitFileStatusDto> fileMap = new TreeMap<>();

            // 1. Conflicting files
            for (String path : status.getConflicting()) {
                fileMap.put(path, new GitFileStatusDto(path, "CONFLICTED", false, true));
            }

            // 2. Staged additions, modifications, removals
            for (String path : status.getAdded()) {
                fileMap.put(path, new GitFileStatusDto(path, "ADDED", true, false));
            }
            for (String path : status.getChanged()) {
                fileMap.put(path, new GitFileStatusDto(path, "MODIFIED", true, false));
            }
            for (String path : status.getRemoved()) {
                fileMap.put(path, new GitFileStatusDto(path, "DELETED", true, false));
            }

            // 3. Unstaged modifications, deletions, untracked
            for (String path : status.getModified()) {
                if (!fileMap.containsKey(path)) {
                    fileMap.put(path, new GitFileStatusDto(path, "MODIFIED", false, false));
                }
            }
            for (String path : status.getMissing()) {
                if (!fileMap.containsKey(path)) {
                    fileMap.put(path, new GitFileStatusDto(path, "DELETED", false, false));
                }
            }
            for (String path : status.getUntracked()) {
                if (!fileMap.containsKey(path)) {
                    fileMap.put(path, new GitFileStatusDto(path, "UNTRACKED", false, false));
                }
            }

            List<GitFileStatusDto> files = new ArrayList<>(fileMap.values());

            int stagedCount = (int) files.stream().filter(GitFileStatusDto::staged).count();
            int unstagedCount = files.size() - stagedCount;
            boolean clean = status.isClean();

            // Calculate ahead / behind relative to tracked upstream
            int ahead = 0;
            int behind = 0;
            try {
                BranchTrackingStatus tracking = BranchTrackingStatus.of(repository, currentBranch);
                if (tracking != null) {
                    ahead = tracking.getAheadCount();
                    behind = tracking.getBehindCount();
                }
            } catch (Exception e) {
                log.debug("Unable to compute tracking status for branch {}: {}", currentBranch, e.getMessage());
            }

            return new GitStatusResponse(currentBranch, ahead, behind, clean, files, stagedCount, unstagedCount);
        } catch (Exception e) {
            log.error("Failed to query Git status for project: {}", projectId, e);
            throw new GitException("Failed to retrieve Git status: " + e.getMessage(), e);
        }
    }
}
