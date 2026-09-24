package com.devpilot.ai.service;

import com.devpilot.ai.dto.project.ProjectRequest;
import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project sampleProject;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleProject = Project.builder()
                .id(sampleId)
                .name("cloud-compiler-service")
                .description("Compiles Java code securely in Docker")
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .framework("Spring Boot 3")
                .status(ProjectStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldReturnAllActiveProjects() {
        when(projectRepository.findByStatus(ProjectStatus.ACTIVE)).thenReturn(List.of(sampleProject));

        List<ProjectResponse> projects = projectService.getAllProjects();
        assertNotNull(projects);
        assertEquals(1, projects.size());
        assertEquals("cloud-compiler-service", projects.get(0).getName());
        verify(projectRepository, times(1)).findByStatus(ProjectStatus.ACTIVE);
    }

    @Test
    void shouldCreateAndPersistProject() {
        ProjectRequest request = ProjectRequest.builder()
                .name("cloud-compiler-service")
                .description("Compiles Java code securely in Docker")
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .framework("Spring Boot 3")
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        ProjectResponse created = projectService.createProject(request);
        assertNotNull(created.getId());
        assertEquals("cloud-compiler-service", created.getName());
        assertEquals("Java", created.getLanguage());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void shouldGetProjectById() {
        when(projectRepository.findByIdAndStatusNot(sampleId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(sampleProject));

        ProjectResponse response = projectService.getProjectById(sampleId);
        assertNotNull(response);
        assertEquals(sampleId, response.getId());
        assertEquals("cloud-compiler-service", response.getName());
    }

    @Test
    void shouldUpdateExistingProject() {
        when(projectRepository.findByIdAndStatusNot(sampleId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        ProjectRequest updateReq = ProjectRequest.builder()
                .name("updated-compiler")
                .description("Refined description")
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .build();

        ProjectResponse updated = projectService.updateProject(sampleId, updateReq);
        assertNotNull(updated);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void shouldSoftDeleteProject() {
        when(projectRepository.findByIdAndStatusNot(sampleId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        projectService.deleteProject(sampleId);

        assertEquals(ProjectStatus.DELETED, sampleProject.getStatus());
        verify(projectRepository, times(1)).save(sampleProject);
    }

    @Test
    void shouldThrowBadRequestWhenNameIsInvalid() {
        ProjectRequest invalidReq = ProjectRequest.builder()
                .name(" ")
                .template(ProjectTemplate.BLANK)
                .language("Java")
                .build();

        assertThrows(BadRequestException.class, () -> projectService.createProject(invalidReq));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldThrowResourceNotFoundWhenNonExistent() {
        UUID nonExistent = UUID.randomUUID();
        when(projectRepository.findByIdAndStatusNot(nonExistent, ProjectStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(nonExistent));
    }
}
