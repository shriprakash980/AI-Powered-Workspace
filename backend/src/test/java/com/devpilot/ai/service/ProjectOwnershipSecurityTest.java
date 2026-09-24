package com.devpilot.ai.service;

import com.devpilot.ai.dto.project.ProjectRequest;
import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectOwnershipSecurityTest {

    @Mock
    private ProjectRepository projectRepository;

    private ProjectService projectService;

    private UUID userAId;
    private UUID userBId;
    private UserPrincipal principalUserA;
    private UserPrincipal principalUserB;
    private UserPrincipal principalAdmin;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository);

        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();

        principalUserA = new UserPrincipal(
                userAId, "User A", "a@test.com", "hash", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        principalUserB = new UserPrincipal(
                userBId, "User B", "b@test.com", "hash", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        principalAdmin = new UserPrincipal(
                UUID.randomUUID(), "Admin", "admin@test.com", "hash", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("User A should be able to get their own project")
    void userAShouldAccessOwnProject() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .id(projectId)
                .name("user-a-project")
                .ownerId(userAId)
                .status(ProjectStatus.ACTIVE)
                .template(ProjectTemplate.HTML_CSS_JS)
                .language("JavaScript")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getProjectById(projectId, principalUserA);

        assertNotNull(response);
        assertEquals(projectId, response.getId());
        assertEquals("user-a-project", response.getName());
    }

    @Test
    @DisplayName("User B should be FORBIDDEN from accessing User A's project")
    void userBShouldBeForbiddenFromUserAProject() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .id(projectId)
                .name("user-a-secret-project")
                .ownerId(userAId)
                .status(ProjectStatus.ACTIVE)
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(project));

        assertThrows(ForbiddenException.class, () ->
                projectService.getProjectById(projectId, principalUserB));
    }

    @Test
    @DisplayName("User B should be FORBIDDEN from updating User A's project")
    void userBShouldBeForbiddenFromUpdatingUserAProject() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .id(projectId)
                .name("user-a-project")
                .ownerId(userAId)
                .status(ProjectStatus.ACTIVE)
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(project));

        ProjectRequest updateRequest = ProjectRequest.builder()
                .name("hacked-name")
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .build();

        assertThrows(ForbiddenException.class, () ->
                projectService.updateProject(projectId, updateRequest, principalUserB));
    }

    @Test
    @DisplayName("Admin should be allowed to access any user's project")
    void adminShouldAccessAnyProject() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .id(projectId)
                .name("user-a-project")
                .ownerId(userAId)
                .status(ProjectStatus.ACTIVE)
                .template(ProjectTemplate.HTML_CSS_JS)
                .language("JavaScript")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getProjectById(projectId, principalAdmin);

        assertNotNull(response);
        assertEquals(projectId, response.getId());
    }
}
