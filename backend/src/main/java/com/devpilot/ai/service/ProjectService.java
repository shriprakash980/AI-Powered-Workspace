package com.devpilot.ai.service;

import com.devpilot.ai.dto.project.ProjectRequest;
import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.devpilot.ai.repository.ProjectFileRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ActivityLogService activityLogService;

    public ProjectService(ProjectRepository projectRepository) {
        this(projectRepository, null, null);
    }

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
                          @Autowired(required = false) ProjectFileRepository projectFileRepository,
                          @Autowired(required = false) ActivityLogService activityLogService) {
        this.projectRepository = projectRepository;
        this.projectFileRepository = projectFileRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return getAllProjects();
        }
        log.info("Fetching projects for user ID: {}", userPrincipal.getId());
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByStatus(ProjectStatus.ACTIVE);
        } else {
            projects = projectRepository.findByOwnerIdAndStatusNot(userPrincipal.getId(), ProjectStatus.DELETED);
        }

        return projects.stream()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(ProjectResponse::getUpdatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all active projects from PostgreSQL repository");
        return projectRepository.findByStatus(ProjectStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(ProjectResponse::getUpdatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id, UserPrincipal userPrincipal) {
        log.info("Fetching project by id: {}", id);
        Project project = findActiveProjectOrThrow(id);
        verifyOwnership(project, userPrincipal);
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        return getProjectById(id, null);
    }

    public ProjectResponse createProject(ProjectRequest request, UserPrincipal userPrincipal) {
        validateProjectRequest(request);

        String trimmedName = request.getName().trim();
        Instant now = Instant.now();
        UUID ownerId = userPrincipal != null ? userPrincipal.getId() : null;

        Project project = Project.builder()
                .name(trimmedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .template(request.getTemplate())
                .language(request.getLanguage().trim())
                .framework(request.getFramework() != null ? request.getFramework().trim() : null)
                .status(ProjectStatus.ACTIVE)
                .ownerId(ownerId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully in PostgreSQL with ID: {} and Name: '{}' (Owner: {})",
                savedProject.getId(), trimmedName, ownerId);

        scaffoldDefaultFiles(savedProject);

        if (activityLogService != null) {
            activityLogService.logActivity(ownerId, savedProject.getId(), "PROJECT_CREATED", "Project created: " + trimmedName);
        }

        return mapToResponse(savedProject);
    }

    public ProjectResponse createProject(ProjectRequest request) {
        return createProject(request, null);
    }

    public ProjectResponse updateProject(UUID id, ProjectRequest request, UserPrincipal userPrincipal) {
        validateProjectRequest(request);
        Project project = findActiveProjectOrThrow(id);
        verifyOwnership(project, userPrincipal);

        project.setName(request.getName().trim());
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription().trim());
        }
        if (request.getTemplate() != null) {
            project.setTemplate(request.getTemplate());
        }
        if (request.getLanguage() != null) {
            project.setLanguage(request.getLanguage().trim());
        }
        if (request.getFramework() != null) {
            project.setFramework(request.getFramework().trim());
        }
        project.setUpdatedAt(Instant.now());

        Project updatedProject = projectRepository.save(project);
        log.info("Updated project ID: {} in PostgreSQL", id);

        if (activityLogService != null) {
            UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
            activityLogService.logActivity(userId, id, "PROJECT_UPDATED", "Project updated: " + updatedProject.getName());
        }

        return mapToResponse(updatedProject);
    }

    public ProjectResponse updateProject(UUID id, ProjectRequest request) {
        return updateProject(id, request, null);
    }

    public void deleteProject(UUID id, UserPrincipal userPrincipal) {
        Project project = findActiveProjectOrThrow(id);
        verifyOwnership(project, userPrincipal);

        project.setStatus(ProjectStatus.DELETED);
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
        log.info("Soft deleted project ID: {} in PostgreSQL", id);

        if (activityLogService != null) {
            UUID userId = userPrincipal != null ? userPrincipal.getId() : null;
            activityLogService.logActivity(userId, id, "PROJECT_DELETED", "Project deleted: " + project.getName());
        }
    }

    public void deleteProject(UUID id) {
        deleteProject(id, null);
    }

    private void scaffoldDefaultFiles(Project project) {
        if (projectFileRepository == null) return;
        try {
            Instant now = Instant.now();
            UUID projectId = project.getId();

            // 1. README.md
            String readmeContent = "# " + project.getName() + "\n\n"
                    + (project.getDescription() != null ? project.getDescription() + "\n\n" : "")
                    + "- **Language:** " + project.getLanguage() + "\n"
                    + "- **Template:** " + project.getTemplate() + "\n\n"
                    + "Welcome to your DevPilot AI workspace!\n";
            projectFileRepository.save(ProjectFile.builder()
                    .projectId(projectId)
                    .name("README.md")
                    .path("README.md")
                    .fileType("markdown")
                    .content(readmeContent)
                    .isDirectory(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            // 2. Starter files based on language/template
            String lang = project.getLanguage().toLowerCase();
            if (lang.contains("html") || lang.contains("web") || lang.contains("javascript") || project.getTemplate() == ProjectTemplate.BLANK) {
                String indexHtml = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>"
                        + project.getName() + "</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Welcome to "
                        + project.getName() + "</h1>\n  <p>Built with DevPilot AI.</p>\n  <script src=\"app.js\"></script>\n</body>\n</html>";
                projectFileRepository.save(ProjectFile.builder()
                        .projectId(projectId)
                        .name("index.html")
                        .path("index.html")
                        .fileType("html")
                        .content(indexHtml)
                        .isDirectory(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

                projectFileRepository.save(ProjectFile.builder()
                        .projectId(projectId)
                        .name("style.css")
                        .path("style.css")
                        .fileType("css")
                        .content("/* DevPilot Styles */\nbody {\n  font-family: sans-serif;\n  margin: 2rem;\n  background: #0b0f14;\n  color: #f8fafc;\n}\n")
                        .isDirectory(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

                projectFileRepository.save(ProjectFile.builder()
                        .projectId(projectId)
                        .name("app.js")
                        .path("app.js")
                        .fileType("javascript")
                        .content("// " + project.getName() + " entry point\nconsole.log('DevPilot workspace active');\n")
                        .isDirectory(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            } else if (lang.contains("java")) {
                projectFileRepository.save(ProjectFile.builder()
                        .projectId(projectId)
                        .name("Main.java")
                        .path("Main.java")
                        .fileType("java")
                        .content("public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from " + project.getName() + "!\");\n    }\n}\n")
                        .isDirectory(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            } else if (lang.contains("python")) {
                projectFileRepository.save(ProjectFile.builder()
                        .projectId(projectId)
                        .name("main.py")
                        .path("main.py")
                        .fileType("python")
                        .content("# DevPilot AI Python Project\n\ndef main():\n    print(\"Hello from " + project.getName() + "!\")\n\nif __name__ == '__main__':\n    main()\n")
                        .isDirectory(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Could not scaffold default files for project {}: {}", project.getId(), e.getMessage());
        }
    }

    private void verifyOwnership(Project project, UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return;
        }
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }

        if (project.getOwnerId() != null && !project.getOwnerId().equals(userPrincipal.getId())) {
            log.warn("Access denied: User ID {} does not own project ID {}", userPrincipal.getId(), project.getId());
            throw new ForbiddenException("You do not have permission to access this project");
        }
    }

    private Project findActiveProjectOrThrow(UUID id) {
        return projectRepository.findByIdAndStatusNot(id, ProjectStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }

    private void validateProjectRequest(ProjectRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Project name cannot be empty");
        }
        if (request.getName().trim().length() < 2 || request.getName().trim().length() > 100) {
            throw new BadRequestException("Project name length must be between 2 and 100 characters");
        }
        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new BadRequestException("Project description length cannot exceed 1000 characters");
        }
        if (request.getTemplate() == null) {
            throw new BadRequestException("A valid project template is required");
        }
        if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
            throw new BadRequestException("Project language is required");
        }
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .template(project.getTemplate())
                .language(project.getLanguage())
                .framework(project.getFramework())
                .status(project.getStatus())
                .ownerId(project.getOwnerId())
                .repositoryUrl(project.getRepositoryUrl())
                .deploymentUrl(project.getDeploymentUrl())
                .gitEnabled(project.isGitEnabled())
                .defaultBranch(project.getDefaultBranch())
                .gitProvider(project.getGitProvider())
                .lastFetchedAt(project.getLastFetchedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
