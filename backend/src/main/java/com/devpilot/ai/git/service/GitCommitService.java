package com.devpilot.ai.git.service;

import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.entity.enums.GitOperationType;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ProjectService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class GitCommitService {

    private static final Logger log = LoggerFactory.getLogger(GitCommitService.class);

    private final GitWorkspaceManager gitWorkspaceManager;
    private final GitService gitService;
    private final ProjectService projectService;

    public GitCommitService(GitWorkspaceManager gitWorkspaceManager,
                            GitService gitService,
                            ProjectService projectService) {
        this.gitWorkspaceManager = gitWorkspaceManager;
        this.gitService = gitService;
        this.projectService = projectService;
    }

    /**
     * Stages specific file paths into the Git index.
     */
    public void stage(UUID projectId, List<String> paths, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);
        if (paths == null || paths.isEmpty()) return;

        gitWorkspaceManager.syncDbFilesToDisk(projectId);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            var addCommand = git.add();
            for (String rawPath : paths) {
                String cleanPath = sanitizePath(rawPath);
                addCommand.addFilepattern(cleanPath);
            }
            addCommand.call();
            log.info("Staged {} files for project ID: {}", paths.size(), projectId);
        } catch (Exception e) {
            log.error("Failed to stage files in project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to stage files: " + e.getMessage(), e);
        }
    }

    /**
     * Unstages specific file paths from the Git index.
     */
    public void unstage(UUID projectId, List<String> paths, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);
        if (paths == null || paths.isEmpty()) return;

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            var resetCommand = git.reset();
            for (String rawPath : paths) {
                String cleanPath = sanitizePath(rawPath);
                resetCommand.addPath(cleanPath);
            }
            resetCommand.call();
            log.info("Unstaged {} files for project ID: {}", paths.size(), projectId);
        } catch (Exception e) {
            log.error("Failed to unstage files in project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to unstage files: " + e.getMessage(), e);
        }
    }

    /**
     * Stages all modified and untracked files into the index.
     */
    public void stageAll(UUID projectId, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);
        gitWorkspaceManager.syncDbFilesToDisk(projectId);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            log.info("Staged all changes for project ID: {}", projectId);
        } catch (Exception e) {
            log.error("Failed to stage all files in project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to stage all files: " + e.getMessage(), e);
        }
    }

    /**
     * Unstages all staged changes from the Git index.
     */
    public void unstageAll(UUID projectId, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            git.reset().call();
            log.info("Unstaged all changes for project ID: {}", projectId);
        } catch (Exception e) {
            log.error("Failed to unstage all files in project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to unstage all files: " + e.getMessage(), e);
        }
    }

    /**
     * Commits currently staged changes with the provided commit message.
     */
    @Transactional
    public GitCommitResponse commit(UUID projectId, GitCommitRequest request, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            throw new GitException("Commit message cannot be empty");
        }
        String commitMessage = request.message().trim();
        if (commitMessage.length() > 1000) {
            throw new GitException("Commit message cannot exceed 1000 characters");
        }

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Status status = git.status().call();
            int stagedCount = status.getAdded().size() + status.getChanged().size() + status.getRemoved().size();
            if (stagedCount == 0) {
                throw new GitException("No staged changes to commit. Please stage changes first.");
            }

            String authorName = userPrincipal != null ? userPrincipal.getUsername() : "DevPilot User";
            String authorEmail = userPrincipal != null ? userPrincipal.getEmail() : "devpilot@workspace.local";

            RevCommit commit = git.commit()
                    .setMessage(commitMessage)
                    .setAuthor(authorName, authorEmail)
                    .setCommitter(authorName, authorEmail)
                    .call();

            String fullHash = commit.getId().getName();
            String shortHash = fullHash.substring(0, Math.min(7, fullHash.length()));
            String branch = git.getRepository().getBranch();

            gitService.logOperation(projectId, userPrincipal != null ? userPrincipal.getId() : null,
                    GitOperationType.COMMIT, branch, fullHash, "SUCCESS", commitMessage);

            log.info("Created commit {} ({}) on branch {} for project {}", shortHash, commitMessage, branch, projectId);
            return new GitCommitResponse(fullHash, shortHash, branch, commitMessage, stagedCount);
        } catch (GitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to commit changes in project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to commit changes: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves paginated commit history.
     */
    public List<GitCommitDto> getCommits(UUID projectId, String branch, int page, int size, String search, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        List<GitCommitDto> result = new ArrayList<>();
        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Repository repo = git.getRepository();

            var logCmd = git.log();
            if (branch != null && !branch.isBlank()) {
                ObjectId branchId = repo.resolve(branch);
                if (branchId != null) {
                    logCmd.add(branchId);
                }
            }

            Iterable<RevCommit> commits = logCmd.call();
            int skip = Math.max(0, page) * Math.max(1, size);
            int count = 0;
            int added = 0;

            String searchLower = search != null ? search.trim().toLowerCase() : null;

            for (RevCommit rev : commits) {
                if (searchLower != null && !searchLower.isEmpty()) {
                    boolean matches = rev.getFullMessage().toLowerCase().contains(searchLower) ||
                            rev.getAuthorIdent().getName().toLowerCase().contains(searchLower);
                    if (!matches) continue;
                }

                if (count++ < skip) continue;

                List<String> parents = new ArrayList<>();
                for (RevCommit p : rev.getParents()) {
                    parents.add(p.getId().getName());
                }

                String fullHash = rev.getId().getName();
                result.add(new GitCommitDto(
                        fullHash,
                        fullHash.substring(0, Math.min(7, fullHash.length())),
                        rev.getFullMessage(),
                        rev.getAuthorIdent().getName(),
                        rev.getAuthorIdent().getEmailAddress(),
                        Instant.ofEpochSecond(rev.getCommitTime()),
                        parents
                ));

                added++;
                if (added >= size) break;
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to load commits for project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to load commit history: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves detailed information and changed files for a single commit.
     */
    public GitCommitDetailDto getCommit(UUID projectId, String commitHash, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Repository repo = git.getRepository();
            ObjectId objId = repo.resolve(commitHash);
            if (objId == null) {
                throw new GitException("Commit not found: " + commitHash);
            }

            try (RevWalk rw = new RevWalk(repo)) {
                RevCommit commit = rw.parseCommit(objId);
                String fullHash = commit.getId().getName();
                List<String> parents = new ArrayList<>();
                for (RevCommit p : commit.getParents()) {
                    parents.add(p.getId().getName());
                }

                List<GitDiffFileDto> diffFiles = getCommitDiffFiles(repo, commit);
                int totalAdd = diffFiles.stream().mapToInt(GitDiffFileDto::additions).sum();
                int totalDel = diffFiles.stream().mapToInt(GitDiffFileDto::deletions).sum();

                return new GitCommitDetailDto(
                        fullHash,
                        fullHash.substring(0, Math.min(7, fullHash.length())),
                        commit.getFullMessage(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        Instant.ofEpochSecond(commit.getCommitTime()),
                        parents,
                        diffFiles,
                        totalAdd,
                        totalDel
                );
            }
        } catch (Exception e) {
            log.error("Failed to inspect commit {} for project {}: {}", commitHash, projectId, e.getMessage());
            throw new GitException("Failed to inspect commit: " + e.getMessage(), e);
        }
    }

    private List<GitDiffFileDto> getCommitDiffFiles(Repository repo, RevCommit commit) throws IOException {
        List<GitDiffFileDto> files = new ArrayList<>();
        try (DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())) {
            df.setRepository(repo);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);

            AbstractTreeIterator oldTreeIter;
            if (commit.getParentCount() > 0) {
                RevCommit parent = commit.getParent(0);
                try (RevWalk rw = new RevWalk(repo)) {
                    RevCommit parsedParent = rw.parseCommit(parent.getId());
                    oldTreeIter = getTreeParser(repo, parsedParent.getTree().getId());
                }
            } else {
                oldTreeIter = new EmptyTreeIterator();
            }

            AbstractTreeIterator newTreeIter = getTreeParser(repo, commit.getTree().getId());
            List<DiffEntry> entries = df.scan(oldTreeIter, newTreeIter);

            for (DiffEntry entry : entries) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter singleDf = new DiffFormatter(out)) {
                    singleDf.setRepository(repo);
                    singleDf.setDiffComparator(RawTextComparator.DEFAULT);
                    singleDf.format(entry);
                }

                int adds = 0;
                int dels = 0;
                for (Edit edit : df.toFileHeader(entry).toEditList()) {
                    adds += edit.getEndB() - edit.getBeginB();
                    dels += edit.getEndA() - edit.getBeginA();
                }

                files.add(new GitDiffFileDto(
                        entry.getNewPath(),
                        entry.getOldPath(),
                        entry.getChangeType().name(),
                        out.toString(StandardCharsets.UTF_8),
                        adds,
                        dels
                ));
            }
        }
        return files;
    }

    /**
     * Computes structured diff for the working directory or staged index.
     */
    public GitDiffResponse getWorkingDiff(UUID projectId, String pathFilter, Boolean staged, String base, UserPrincipal userPrincipal) {
        projectService.getProjectById(projectId, userPrincipal);
        gitWorkspaceManager.syncDbFilesToDisk(projectId);

        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            Repository repo = git.getRepository();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<GitDiffFileDto> result = new ArrayList<>();

            try (DiffFormatter df = new DiffFormatter(out)) {
                df.setRepository(repo);
                df.setDiffComparator(RawTextComparator.DEFAULT);
                df.setDetectRenames(true);

                if (pathFilter != null && !pathFilter.isBlank()) {
                    df.setPathFilter(PathFilter.create(sanitizePath(pathFilter)));
                }

                List<DiffEntry> diffs;
                if (Boolean.TRUE.equals(staged)) {
                    // Staged: compare HEAD tree with index
                    AbstractTreeIterator headTree = getHeadTree(repo);
                    AbstractTreeIterator indexTree = new org.eclipse.jgit.dircache.DirCacheIterator(repo.readDirCache());
                    diffs = df.scan(headTree, indexTree);
                } else {
                    // Working tree: compare index with working tree
                    AbstractTreeIterator indexTree = new org.eclipse.jgit.dircache.DirCacheIterator(repo.readDirCache());
                    FileTreeIterator workTree = new FileTreeIterator(repo);
                    diffs = df.scan(indexTree, workTree);
                }

                for (DiffEntry entry : diffs) {
                    ByteArrayOutputStream singleOut = new ByteArrayOutputStream();
                    try (DiffFormatter singleDf = new DiffFormatter(singleOut)) {
                        singleDf.setRepository(repo);
                        singleDf.setDiffComparator(RawTextComparator.DEFAULT);
                        singleDf.format(entry);
                    }

                    int adds = 0;
                    int dels = 0;
                    for (Edit edit : df.toFileHeader(entry).toEditList()) {
                        adds += edit.getEndB() - edit.getBeginB();
                        dels += edit.getEndA() - edit.getBeginA();
                    }

                    result.add(new GitDiffFileDto(
                            entry.getNewPath(),
                            entry.getOldPath(),
                            entry.getChangeType().name(),
                            singleOut.toString(StandardCharsets.UTF_8),
                            adds,
                            dels
                    ));
                }
            }

            int totalAdd = result.stream().mapToInt(GitDiffFileDto::additions).sum();
            int totalDel = result.stream().mapToInt(GitDiffFileDto::deletions).sum();
            return new GitDiffResponse(result, totalAdd, totalDel);
        } catch (Exception e) {
            log.error("Failed to generate working diff for project {}: {}", projectId, e.getMessage());
            throw new GitException("Failed to generate diff: " + e.getMessage(), e);
        }
    }

    private AbstractTreeIterator getHeadTree(Repository repo) throws IOException {
        ObjectId head = repo.resolve("HEAD^{tree}");
        if (head == null) {
            return new EmptyTreeIterator();
        }
        return getTreeParser(repo, head);
    }

    private AbstractTreeIterator getTreeParser(Repository repo, ObjectId treeId) throws IOException {
        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(reader, treeId);
            return treeParser;
        }
    }

    private String sanitizePath(String path) {
        if (path == null) return "";
        String p = path.trim().replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        if (p.contains("..")) {
            throw new GitException("Path traversal rejected in path: " + path);
        }
        return p;
    }
}
