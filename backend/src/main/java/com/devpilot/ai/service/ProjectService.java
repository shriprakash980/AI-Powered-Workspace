package com.devpilot.ai.service;

import com.devpilot.ai.dto.project.ProjectRequest;
import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
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
    public ProjectResponse getProjectById(UUID id) {
        log.info("Fetching project by id: {}", id);
        Project project = findActiveProjectOrThrow(id);
        return mapToResponse(project);
    }

    public ProjectResponse createProject(ProjectRequest request) {
        validateProjectRequest(request);

        String trimmedName = request.getName().trim();
        Instant now = Instant.now();

        Project project = Project.builder()
                .name(trimmedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .template(request.getTemplate())
                .language(request.getLanguage().trim())
                .framework(request.getFramework() != null ? request.getFramework().trim() : null)
                .status(ProjectStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully in PostgreSQL with ID: {} and Name: '{}'", savedProject.getId(), trimmedName);
        return mapToResponse(savedProject);
    }

    public ProjectResponse updateProject(UUID id, ProjectRequest request) {
        validateProjectRequest(request);
        Project project = findActiveProjectOrThrow(id);

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
        return mapToResponse(updatedProject);
    }

    public void deleteProject(UUID id) {
        Project project = findActiveProjectOrThrow(id);
        project.setStatus(ProjectStatus.DELETED);
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
        log.info("Soft deleted project ID: {} in PostgreSQL", id);
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
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
