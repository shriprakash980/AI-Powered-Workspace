package com.devpilot.ai.git.service;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.entity.GitHubConnection;
import com.devpilot.ai.git.entity.enums.GitOperationType;
import com.devpilot.ai.git.exception.GitConflictException;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.git.repository.GitHubConnectionRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.git.security.TokenEncryptionService;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ProjectService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@Service
public class GitRemoteService {

    private static final Logger log = LoggerFactory.getLogger(GitRemoteService.class);

    private final GitWorkspaceManager gitWorkspaceManager;
    private final GitService gitService;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final GitHubConnectionRepository gitHubConnectionRepository;
    private final TokenEncryptionService tokenEncryptionService;

    public GitRemoteService(GitWorkspaceManager gitWorkspaceManager,
                            GitService gitService,
                            ProjectService projectService,
                            ProjectRepository projectRepository,
                            GitHubConnectionRepository gitHubConnectionRepository,
                            TokenEncryptionService tokenEncryptionService) {
        this.gitWorkspaceManager = gitWorkspaceManager;
        this.gitService = gitService;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.gitHubConnectionRepository = gitHubConnectionRepository;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    /**
     * Lists remotes for a project with credentials masked.
     */
    public List<GitRemoteDto> getRemotes(UUID projectId, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            List<RemoteConfig> remoteConfigs = git.remoteList().call();
            List<GitRemoteDto> remotes = new ArrayList<>();

            for (RemoteConfig rc : remoteConfigs) {
                String fetchUrl = rc.getURIs().isEmpty() ? "" : maskCredentials(rc.getURIs().get(0).toString());
                String pushUrl = rc.getPushURIs().isEmpty() ? fetchUrl : maskCredentials(rc.getPushURIs().get(0).toString());
                remotes.add(new GitRemoteDto(rc.getName(), fetchUrl, pushUrl));
            }

            return remotes;
        } catch (Exception e) {
            log.error("Failed to list remotes for project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to list remotes: " + e.getMessage(), e);
        }
    }

    /**
     * Adds or updates a remote for the project.
     */
    @Transactional
    public GitRemoteDto addRemote(UUID projectId, GitAddRemoteRequest request, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        if (request == null || request.repositoryUrl() == null || request.repositoryUrl().isBlank()) {
            throw new GitException("Repository URL cannot be empty");
        }
        String remoteName = (request.name() != null && !request.name().isBlank()) ? request.name().trim() : "origin";
        String rawUrl = request.repositoryUrl().trim();
        validateRepositoryUrl(rawUrl);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            URIish uri = new URIish(rawUrl);
            git.remoteAdd()
                    .setName(remoteName)
                    .setUri(uri)
                    .call();

            project.setRepositoryUrl(maskCredentials(rawUrl));
            projectRepository.save(project);

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.REMOTE_ADD, null, null, "SUCCESS", "Added remote " + remoteName + ": " + maskCredentials(rawUrl));

            log.info("Added remote '{}' ({}) for project ID: {}", remoteName, maskCredentials(rawUrl), projectId);
            return new GitRemoteDto(remoteName, maskCredentials(rawUrl), maskCredentials(rawUrl));
        } catch (Exception e) {
            log.error("Failed to add remote for project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to add remote: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches refs from remote.
     */
    @Transactional
    public GitFetchResponse fetch(UUID projectId, String remoteName, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        String remote = (remoteName != null && !remoteName.isBlank()) ? remoteName.trim() : "origin";

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            var fetchCmd = git.fetch().setRemote(remote);
            CredentialsProvider credentials = resolveCredentials(userPrincipal);
            if (credentials != null) {
                fetchCmd.setCredentialsProvider(credentials);
            }

            FetchResult result = fetchCmd.call();
            List<String> updated = new ArrayList<>();
            for (TrackingRefUpdate update : result.getTrackingRefUpdates()) {
                updated.add(update.getLocalName() + " (" + update.getResult().name() + ")");
            }

            project.setLastFetchedAt(Instant.now());
            projectRepository.save(project);

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.FETCH, null, null, "SUCCESS", "Fetched " + updated.size() + " refs from " + remote);

            log.info("Fetched {} refs from remote '{}' for project ID: {}", updated.size(), remote, projectId);
            return new GitFetchResponse(remote, updated, Instant.now(), "Fetched successfully from " + remote);
        } catch (Exception e) {
            log.error("Failed to fetch from remote '{}' in project {}: {}", remote, projectId, e.getMessage());
            throw new GitException("Failed to fetch from remote: " + e.getMessage(), e);
        }
    }

    /**
     * Pulls latest changes from remote branch. Detects merge conflicts.
     */
    @Transactional
    public GitPullResponse pull(UUID projectId, String remoteName, String branch, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        String remote = (remoteName != null && !remoteName.isBlank()) ? remoteName.trim() : "origin";

        // Sync local DB changes to disk first
        gitWorkspaceManager.syncDbFilesToDisk(projectId);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            var pullCmd = git.pull().setRemote(remote);
            if (branch != null && !branch.isBlank()) {
                pullCmd.setRemoteBranchName(branch.trim());
            }

            CredentialsProvider credentials = resolveCredentials(userPrincipal);
            if (credentials != null) {
                pullCmd.setCredentialsProvider(credentials);
            }

            PullResult pullResult = pullCmd.call();
            if (!pullResult.isSuccessful()) {
                MergeResult mergeResult = pullResult.getMergeResult();
                if (mergeResult != null && mergeResult.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING) {
                    List<GitConflictFileDto> conflicts = new ArrayList<>();
                    if (mergeResult.getConflicts() != null) {
                        for (String path : mergeResult.getConflicts().keySet()) {
                            conflicts.add(new GitConflictFileDto(path, true));
                        }
                    }

                    gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                            GitOperationType.PULL, branch, null, "CONFLICTED", "Merge conflicts in " + conflicts.size() + " files");

                    log.warn("Pull generated merge conflicts in project {}: {} files", projectId, conflicts.size());
                    return new GitPullResponse("CONFLICTED", conflicts, 0, "Merge conflicts detected. Please resolve conflicts before committing.");
                }
            }

            // Sync disk files back to database
            gitWorkspaceManager.syncDiskFilesToDb(projectId, project);
            project.setLastFetchedAt(Instant.now());
            projectRepository.save(project);

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.PULL, branch, null, "SUCCESS", "Pull completed successfully");

            log.info("Pull completed successfully for project ID: {}", projectId);
            return new GitPullResponse("SUCCESS", Collections.emptyList(), 1, "Pulled changes successfully from " + remote);
        } catch (Exception e) {
            log.error("Failed to pull changes for project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to pull from remote: " + e.getMessage(), e);
        }
    }

    /**
     * Pushes local commits to the configured remote repository.
     */
    @Transactional
    public GitPushResponse push(UUID projectId, String remoteName, String branch, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        String remote = (remoteName != null && !remoteName.isBlank()) ? remoteName.trim() : "origin";

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            String targetBranch = (branch != null && !branch.isBlank()) ? branch.trim() : git.getRepository().getBranch();

            PushCommand pushCmd = git.push()
                    .setRemote(remote)
                    .add(targetBranch);

            CredentialsProvider credentials = resolveCredentials(userPrincipal);
            if (credentials != null) {
                pushCmd.setCredentialsProvider(credentials);
            }

            Iterable<PushResult> results = pushCmd.call();
            int pushedCount = 0;
            String statusDesc = "UP_TO_DATE";

            for (PushResult result : results) {
                for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                    if (update.getStatus() == RemoteRefUpdate.Status.OK) {
                        pushedCount++;
                        statusDesc = "OK";
                    } else if (update.getStatus() == RemoteRefUpdate.Status.UP_TO_DATE) {
                        statusDesc = "UP_TO_DATE";
                    } else if (update.getStatus() == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                        throw new GitException("Push rejected: remote has newer commits (non-fast-forward). Pull remote changes first.");
                    } else {
                        throw new GitException("Push failed: " + update.getStatus().name() + " - " + update.getMessage());
                    }
                }
            }

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.PUSH, targetBranch, null, "SUCCESS", "Pushed branch " + targetBranch + " to " + remote);

            log.info("Pushed branch '{}' to remote '{}' in project ID: {}", targetBranch, remote, projectId);
            return new GitPushResponse(targetBranch, remote, pushedCount, statusDesc, "Pushed branch " + targetBranch + " successfully to " + remote);
        } catch (GitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to push changes for project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to push to remote: " + e.getMessage(), e);
        }
    }

    private CredentialsProvider resolveCredentials(UserPrincipal userPrincipal) {
        if (userPrincipal == null) return null;
        Optional<GitHubConnection> conn = gitHubConnectionRepository.findByUserIdAndRevokedAtIsNull(userPrincipal.getId());
        if (conn.isPresent() && conn.get().getAccessTokenEncrypted() != null) {
            String token = tokenEncryptionService.decrypt(conn.get().getAccessTokenEncrypted());
            if (token != null && !token.isBlank()) {
                return new UsernamePasswordCredentialsProvider(token, "");
            }
        }
        return null;
    }

    private void validateRepositoryUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new GitException("Repository URL cannot be empty");
        }
        String u = url.trim();
        if (u.startsWith("file://") || u.startsWith("/") || u.startsWith("C:") || u.startsWith("c:")) {
            throw new GitException("Invalid repository URL: Local filesystem Git URLs are strictly prohibited");
        }
        if (!u.startsWith("https://") && !u.startsWith("http://") && !u.startsWith("git@")) {
            throw new GitException("Repository URL must use https:// or SSH format");
        }
    }

    private String maskCredentials(String url) {
        if (url == null) return "";
        try {
            URI uri = URI.create(url);
            if (uri.getUserInfo() != null) {
                return url.replace(uri.getUserInfo() + "@", "***@");
            }
        } catch (Exception ignored) {
            // Not standard URI (e.g. git@github.com:...)
        }
        return url;
    }
}
