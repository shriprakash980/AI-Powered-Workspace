package com.devpilot.ai.git.service;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.repository.ProjectFileRepository;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
public class GitWorkspaceManager {

    private static final Logger log = LoggerFactory.getLogger(GitWorkspaceManager.class);

    private final Path workspaceRoot;
    private final long maxRepoSizeMb;
    private final int maxFileCount;
    private final long maxFileSizeMb;
    private final ProjectFileRepository projectFileRepository;

    public GitWorkspaceManager(
            @Value("${app.git.workspace-root:${java.io.tmpdir}/devpilot-workspaces}") String rawRoot,
            @Value("${app.git.max-repo-size-mb:500}") long maxRepoSizeMb,
            @Value("${app.git.max-file-count:10000}") int maxFileCount,
            @Value("${app.git.max-file-size-mb:25}") long maxFileSizeMb,
            ProjectFileRepository projectFileRepository) {
        this.workspaceRoot = Path.of(rawRoot).toAbsolutePath().normalize();
        this.maxRepoSizeMb = maxRepoSizeMb;
        this.maxFileCount = maxFileCount;
        this.maxFileSizeMb = maxFileSizeMb;
        this.projectFileRepository = projectFileRepository;

        try {
            Files.createDirectories(this.workspaceRoot);
            log.info("Initialized DevPilot Git workspace root at: {}", this.workspaceRoot);
        } catch (IOException e) {
            log.error("Failed to create Git workspace root: {}", this.workspaceRoot, e);
        }
    }

    /**
     * Resolves and securely validates the project's isolated workspace directory.
     */
    public File getProjectDirectory(UUID projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        Path projectPath = workspaceRoot.resolve(projectId.toString()).normalize();
        if (!projectPath.startsWith(workspaceRoot)) {
            throw new GitException("Path traversal violation: Target directory outside workspace root");
        }
        File dir = projectPath.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Validates that a file path is strictly located within the project's workspace.
     */
    public File resolveAndValidatePath(UUID projectId, String relativePath) {
        File projectDir = getProjectDirectory(projectId);
        if (relativePath == null || relativePath.isBlank()) {
            return projectDir;
        }

        // Sanitize path
        String cleanPath = relativePath.trim().replace('\\', '/');
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        Path resolved = projectDir.toPath().resolve(cleanPath).normalize();
        if (!resolved.startsWith(projectDir.toPath())) {
            throw new GitException("Security violation: Path escapes project directory: " + relativePath);
        }

        return resolved.toFile();
    }

    /**
     * Opens an existing JGit repository for a project, or throws GitException if not initialized.
     */
    public Git openGit(UUID projectId) {
        File projectDir = getProjectDirectory(projectId);
        File gitDir = new File(projectDir, ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            throw new GitException("Git repository is not initialized for this project. Please initialize or clone first.");
        }
        try {
            return Git.open(projectDir);
        } catch (IOException e) {
            throw new GitException("Failed to open Git repository: " + e.getMessage(), e);
        }
    }

    /**
     * Materializes all database ProjectFiles to the disk workspace before running Git operations.
     */
    public void syncDbFilesToDisk(UUID projectId) {
        File projectDir = getProjectDirectory(projectId);
        List<ProjectFile> dbFiles = projectFileRepository.findByProjectId(projectId);

        Set<String> activePaths = new HashSet<>();

        for (ProjectFile file : dbFiles) {
            if (file.isDirectory()) {
                continue;
            }
            String relPath = file.getPath();
            if (relPath.startsWith("/")) {
                relPath = relPath.substring(1);
            }
            activePaths.add(relPath.replace('\\', '/'));

            File diskFile = resolveAndValidatePath(projectId, relPath);
            File parentDir = diskFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try {
                String content = file.getContent() != null ? file.getContent() : "";
                // Only write if file doesn't exist or content changed
                if (!diskFile.exists() || !Files.readString(diskFile.toPath(), StandardCharsets.UTF_8).equals(content)) {
                    Files.writeString(diskFile.toPath(), content, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                log.warn("Failed to write DB file to disk for Git sync: {}", relPath, e);
            }
        }
    }

    private void cleanOrphanedDiskFiles(File root, File current, Set<String> activePaths) {
        File[] children = current.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.getName().equals(".git")) {
                continue; // Never delete .git
            }
            if (child.isDirectory()) {
                cleanOrphanedDiskFiles(root, child, activePaths);
                // Clean empty dirs if not root
                if (!child.equals(root)) {
                    File[] remaining = child.listFiles();
                    if (remaining == null || remaining.length == 0) {
                        child.delete();
                    }
                }
            } else {
                String relPath = root.toPath().relativize(child.toPath()).toString().replace('\\', '/');
                if (!activePaths.contains(relPath)) {
                    child.delete();
                }
            }
        }
    }

    /**
     * Synchronizes disk files back to the database ProjectFiles after Git operations (pull, checkout, clone).
     */
    @Transactional
    public void syncDiskFilesToDb(UUID projectId, Project project) {
        File projectDir = getProjectDirectory(projectId);
        List<ProjectFile> existingFiles = projectFileRepository.findByProjectId(projectId);
        Map<String, ProjectFile> existingByPath = new HashMap<>();
        for (ProjectFile pf : existingFiles) {
            String path = pf.getPath();
            if (!path.startsWith("/")) path = "/" + path;
            existingByPath.put(path, pf);
        }

        Set<String> diskPaths = new HashSet<>();
        List<ProjectFile> toSave = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(projectDir.toPath())) {
            int fileCount = 0;
            long totalBytes = 0;

            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(path)) {
                    continue;
                }

                String rel = projectDir.toPath().relativize(path).toString().replace('\\', '/');
                if (rel.startsWith(".git/") || rel.equals(".git") || rel.contains("/.git/")) {
                    continue;
                }

                fileCount++;
                if (fileCount > maxFileCount) {
                    throw new GitException("Repository file count exceeds maximum permitted limit (" + maxFileCount + ")");
                }

                long size = Files.size(path);
                totalBytes += size;
                if (size > maxFileSizeMb * 1024 * 1024) {
                    throw new GitException("File " + rel + " exceeds max size limit of " + maxFileSizeMb + "MB");
                }
                if (totalBytes > maxRepoSizeMb * 1024 * 1024) {
                    throw new GitException("Total repository size exceeds max limit of " + maxRepoSizeMb + "MB");
                }

                String dbPath = "/" + rel;
                diskPaths.add(dbPath);

                String content;
                try {
                    content = Files.readString(path, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    content = "[Binary or unsupported file encoding]";
                }

                String fileName = path.getFileName().toString();
                String ext = getFileExtension(fileName);

                ProjectFile pf = existingByPath.get(dbPath);
                if (pf != null) {
                    pf.setContent(content);
                    pf.setName(fileName);
                    pf.setFileType(ext);
                    toSave.add(pf);
                } else {
                    ProjectFile newPf = ProjectFile.builder()
                            .projectId(projectId)
                            .name(fileName)
                            .path(dbPath)
                            .fileType(ext)
                            .content(content)
                            .isDirectory(false)
                            .build();
                    toSave.add(newPf);
                }
            }
        } catch (IOException e) {
            throw new GitException("Failed to scan disk files for project: " + e.getMessage(), e);
        }

        // Delete files from DB that were removed on disk
        for (ProjectFile pf : existingFiles) {
            if (!pf.isDirectory() && !diskPaths.contains(pf.getPath())) {
                projectFileRepository.delete(pf);
            }
        }

        if (!toSave.isEmpty()) {
            projectFileRepository.saveAll(toSave);
        }

        // Also ensure directory records exist in DB
        ensureDirectoryRecords(projectId, diskPaths);
    }

    private void ensureDirectoryRecords(UUID projectId, Set<String> filePaths) {
        Set<String> neededDirs = new HashSet<>();
        for (String fp : filePaths) {
            String dir = getParentDirectory(fp);
            while (dir != null && !dir.isEmpty() && !dir.equals("/")) {
                neededDirs.add(dir);
                dir = getParentDirectory(dir);
            }
        }

        List<ProjectFile> existing = projectFileRepository.findByProjectId(projectId);
        Set<String> existingDirs = new HashSet<>();
        for (ProjectFile pf : existing) {
            if (pf.isDirectory()) {
                existingDirs.add(pf.getPath());
            }
        }

        List<ProjectFile> newDirs = new ArrayList<>();
        for (String dirPath : neededDirs) {
            if (!existingDirs.contains(dirPath)) {
                String dirName = dirPath.substring(dirPath.lastIndexOf('/') + 1);
                ProjectFile dir = ProjectFile.builder()
                        .projectId(projectId)
                        .name(dirName)
                        .path(dirPath)
                        .isDirectory(true)
                        .fileType("directory")
                        .build();
                newDirs.add(dir);
            }
        }

        if (!newDirs.isEmpty()) {
            projectFileRepository.saveAll(newDirs);
        }
    }

    private String getParentDirectory(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return null;
        return path.substring(0, lastSlash);
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0 && idx < filename.length() - 1) ? filename.substring(idx + 1).toLowerCase() : "txt";
    }

    /**
     * Secures JGit configuration against untrusted hooks or arbitrary execution.
     */
    public void secureGitConfig(Git git) {
        try {
            var config = git.getRepository().getConfig();
            // Block execution of hooks on host machine
            config.setString("core", null, "hooksPath", "/dev/null");
            config.setBoolean("core", null, "filemode", false);
            config.save();
        } catch (IOException e) {
            log.warn("Failed to set secure Git repository config: {}", e.getMessage());
        }
    }
}
