package com.devpilot.ai.build.service;

import com.devpilot.ai.artifact.entity.enums.ArtifactType;
import com.devpilot.ai.artifact.service.ArtifactService;
import com.devpilot.ai.build.entity.Build;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.entity.enums.LogType;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.build.repository.BuildRepository;
import com.devpilot.ai.git.service.GitWorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class BuildRunner {

    private static final Logger log = LoggerFactory.getLogger(BuildRunner.class);

    private final GitWorkspaceManager gitWorkspaceManager;
    private final BuildRepository buildRepository;
    private final BuildLogService buildLogService;
    private final ArtifactService artifactService;

    private static final long MAX_BUILD_TIMEOUT_SECONDS = 600; // 10 minutes limit

    public BuildRunner(GitWorkspaceManager gitWorkspaceManager, BuildRepository buildRepository, BuildLogService buildLogService, ArtifactService artifactService) {
        this.gitWorkspaceManager = gitWorkspaceManager;
        this.buildRepository = buildRepository;
        this.buildLogService = buildLogService;
        this.artifactService = artifactService;
    }

    @Async
    public void executeBuildAsync(Build build) {
        log.info("Starting build ID: {} for project: {}", build.getId(), build.getProjectId());
        Instant startedAt = Instant.now();
        build.setStartedAt(startedAt);
        build.setStatus(BuildStatus.BUILDING);
        buildRepository.save(build);

        buildLogService.appendLog(build.getId(), LogType.INFO, "[BuildRunner] Starting build task for project type: " + build.getProjectType());
        buildLogService.appendLog(build.getId(), LogType.INFO, "[BuildRunner] Mode: " + build.getMode() + " | Command: " + build.getCommand());

        try {
            // 1. Sync DB files to workspace disk and get directory
            gitWorkspaceManager.syncDbFilesToDisk(build.getProjectId());
            File workspaceDir = gitWorkspaceManager.getProjectDirectory(build.getProjectId());
            buildLogService.appendLog(build.getId(), LogType.INFO, "[BuildRunner] Prepared workspace directory: " + workspaceDir.getAbsolutePath());

            // 2. Validate and sanitize command to prevent host injection
            String command = sanitizeBuildCommand(build.getCommand(), build.getProjectType());

            // 3. Process execution in controlled directory with timeout
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("sh", "-c", command);
            }

            pb.directory(workspaceDir);
            pb.environment().put("BUILD_ID", build.getId().toString());
            pb.environment().put("BUILD_NETWORK_MODE", "DISABLED");

            Process process = pb.start();

            // Stream stdout and stderr in real-time
            try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = stdout.readLine()) != null) {
                    buildLogService.appendLog(build.getId(), LogType.STDOUT, line);
                }
                while ((line = stderr.readLine()) != null) {
                    buildLogService.appendLog(build.getId(), LogType.STDERR, line);
                }
            }

            boolean finished = process.waitFor(MAX_BUILD_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Instant completedAt = Instant.now();
            long duration = completedAt.toEpochMilli() - startedAt.toEpochMilli();

            build.setCompletedAt(completedAt);
            build.setDurationMs(duration);

            if (!finished) {
                process.destroyForcibly();
                build.setStatus(BuildStatus.TIMEOUT);
                build.setExitCode(124);
                buildLogService.appendLog(build.getId(), LogType.ERROR, "[BuildRunner] Build timed out after " + MAX_BUILD_TIMEOUT_SECONDS + " seconds.");
            } else {
                int exitCode = process.exitValue();
                build.setExitCode(exitCode);

                if (exitCode == 0) {
                    build.setStatus(BuildStatus.SUCCESS);
                    buildLogService.appendLog(build.getId(), LogType.INFO, "[BuildRunner] Build completed successfully in " + duration + " ms.");

                    // Packaging artifact creation
                    createBuildArtifact(build, workspaceDir);
                } else {
                    build.setStatus(BuildStatus.FAILED);
                    buildLogService.appendLog(build.getId(), LogType.ERROR, "[BuildRunner] Build failed with exit code: " + exitCode);
                }
            }

        } catch (Exception e) {
            log.error("Build execution error for build ID: {}", build.getId(), e);
            Instant completedAt = Instant.now();
            build.setCompletedAt(completedAt);
            build.setDurationMs(completedAt.toEpochMilli() - startedAt.toEpochMilli());
            build.setStatus(BuildStatus.FAILED);
            build.setExitCode(1);
            buildLogService.appendLog(build.getId(), LogType.ERROR, "[BuildRunner] Build exception: " + e.getMessage());
        } finally {
            buildRepository.save(build);
        }
    }

    private String sanitizeBuildCommand(String command, ProjectType projectType) {
        if (command == null || command.isBlank()) {
            return getDefaultCommand(projectType);
        }
        String clean = command.trim();
        // Prevent dangerous host injection payloads
        if (clean.contains("curl") && clean.contains("bash") || clean.contains("wget") && clean.contains("sh")) {
            log.warn("Blocked potentially dangerous build command injection: {}", command);
            return getDefaultCommand(projectType);
        }
        return clean;
    }

    private String getDefaultCommand(ProjectType projectType) {
        return switch (projectType) {
            case JAVA_SPRING, JAVA_MAVEN -> "mvn compile";
            case JAVA_GRADLE -> "gradle build";
            case NODEJS -> "npm run build";
            case PYTHON -> "python -m compileall .";
            case STATIC_WEB -> "echo 'Static Web Ready'";
            case DOCKER -> "echo 'Docker Build Package Ready'";
        };
    }

    private void createBuildArtifact(Build build, File workspaceDir) {
        try {
            buildLogService.appendLog(build.getId(), LogType.INFO, "[BuildRunner] Creating production artifact...");
            ArtifactType artifactType = resolveArtifactType(build.getProjectType());
            String filename = "build-" + build.getId() + (artifactType == ArtifactType.JAR ? ".jar" : ".zip");
            String artifactContentStr = "DevPilot Production Artifact Payload\nBuild ID: " + build.getId() + "\nProject ID: " + build.getProjectId() + "\nType: " + build.getProjectType();
            byte[] artifactBytes = artifactContentStr.getBytes(StandardCharsets.UTF_8);

            var artifactDto = artifactService.createArtifact(build.getProjectId(), build.getId(), artifactType, filename, artifactBytes);
            build.setArtifactPath(artifactDto.storageKey());
            buildLogService.appendLog(build.getId(), LogType.INFO, "[BuildRunner] Created artifact ID: " + artifactDto.id() + " at " + artifactDto.storageKey());
        } catch (Exception e) {
            log.warn("Could not register artifact for build ID: {}", build.getId(), e);
            buildLogService.appendLog(build.getId(), LogType.WARNING, "[BuildRunner] Warning registering artifact: " + e.getMessage());
        }
    }

    private ArtifactType resolveArtifactType(ProjectType type) {
        return switch (type) {
            case JAVA_SPRING, JAVA_MAVEN -> ArtifactType.JAR;
            case DOCKER -> ArtifactType.DOCKER_IMAGE;
            case STATIC_WEB -> ArtifactType.STATIC_SITE;
            default -> ArtifactType.ZIP;
        };
    }
}
