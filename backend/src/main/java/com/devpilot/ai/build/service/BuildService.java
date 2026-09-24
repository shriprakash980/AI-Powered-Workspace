package com.devpilot.ai.build.service;

import com.devpilot.ai.build.dto.BuildRequest;
import com.devpilot.ai.build.dto.BuildResponse;
import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.Build;
import com.devpilot.ai.build.entity.enums.BuildMode;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.build.repository.BuildRepository;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.git.service.GitWorkspaceManager;
import com.devpilot.ai.service.ProjectService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildService.class);

    private final BuildRepository buildRepository;
    private final BuildDetectionService buildDetectionService;
    private final BuildRunner buildRunner;
    private final ProjectService projectService;
    private final GitWorkspaceManager gitWorkspaceManager;

    private static final int MAX_RUNNING_BUILDS_PER_USER = 5;

    public BuildService(BuildRepository buildRepository,
                        BuildDetectionService buildDetectionService,
                        BuildRunner buildRunner,
                        ProjectService projectService,
                        GitWorkspaceManager gitWorkspaceManager) {
        this.buildRepository = buildRepository;
        this.buildDetectionService = buildDetectionService;
        this.buildRunner = buildRunner;
        this.projectService = projectService;
        this.gitWorkspaceManager = gitWorkspaceManager;
    }

    @Transactional
    public BuildResponse triggerBuild(UUID projectId, User user, BuildRequest request) {
        Project project = projectService.getProjectEntityByIdAndUser(projectId, user.getId());

        long activeBuilds = buildRepository.countByUserIdAndStatus(user.getId(), BuildStatus.BUILDING);
        if (activeBuilds >= MAX_RUNNING_BUILDS_PER_USER) {
            throw new IllegalStateException("Maximum active builds limit (" + MAX_RUNNING_BUILDS_PER_USER + ") reached for user.");
        }

        ProjectDetectionResponse detection = buildDetectionService.detectProjectType(projectId);
        ProjectType type = detection.primaryType();

        String command = request.customCommand();
        if (command == null || command.isBlank()) {
            command = switch (request.mode()) {
                case TEST -> detection.suggestedTestCommand();
                case BUILD_AND_TEST -> detection.suggestedBuildCommand() + " && " + detection.suggestedTestCommand();
                default -> detection.suggestedBuildCommand();
            };
        }

        String commitHash = getLatestCommitHash(projectId);

        Build build = new Build();
        build.setProjectId(projectId);
        build.setUserId(user.getId());
        build.setStatus(BuildStatus.BUILDING);
        build.setProjectType(type);
        build.setMode(request.mode() != null ? request.mode() : BuildMode.BUILD);
        build.setCommand(command);
        build.setCommitHash(commitHash);

        Build savedBuild = buildRepository.save(build);
        log.info("Queued build ID: {} for project ID: {}", savedBuild.getId(), projectId);

        buildRunner.executeBuildAsync(savedBuild);

        return mapToResponse(savedBuild);
    }

    @Transactional(readOnly = true)
    public Page<BuildResponse> getProjectBuilds(UUID projectId, User user, int page, int size) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        return buildRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BuildResponse getBuildDetails(UUID projectId, Long buildId, User user) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        Build build = buildRepository.findById(UUID.fromString(buildId.toString()))
                .orElseThrow(() -> new IllegalArgumentException("Build not found: " + buildId));
        return mapToResponse(build);
    }

    @Transactional(readOnly = true)
    public BuildResponse getBuildDetailsByUuid(UUID projectId, UUID buildId, User user) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new IllegalArgumentException("Build not found: " + buildId));
        return mapToResponse(build);
    }

    @Transactional
    public BuildResponse cancelBuild(UUID projectId, UUID buildId, User user) {
        projectService.verifyProjectOwnership(projectId, user.getId());
        Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new IllegalArgumentException("Build not found: " + buildId));

        if (build.getStatus() == BuildStatus.BUILDING || build.getStatus() == BuildStatus.QUEUED) {
            build.setStatus(BuildStatus.CANCELLED);
            buildRepository.save(build);
            log.info("Cancelled build ID: {}", buildId);
        }

        return mapToResponse(build);
    }

    private String getLatestCommitHash(UUID projectId) {
        if (gitWorkspaceManager == null) return null;
        try (Git git = gitWorkspaceManager.openGit(projectId)) {
            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            return head != null ? head.getName() : null;
        } catch (Exception e) {
            log.debug("Could not resolve commit hash for project ID: {}", projectId);
            return null;
        }
    }

    public BuildResponse mapToResponse(Build build) {
        return new BuildResponse(
                build.getId(),
                build.getProjectId(),
                build.getUserId(),
                build.getStatus(),
                build.getProjectType(),
                build.getMode(),
                build.getCommand(),
                build.getCommitHash(),
                build.getArtifactPath(),
                build.getExitCode(),
                build.getDurationMs(),
                build.getStartedAt(),
                build.getCompletedAt(),
                build.getCreatedAt()
        );
    }
}
