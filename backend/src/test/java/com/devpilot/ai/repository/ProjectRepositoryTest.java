package com.devpilot.ai.repository;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void shouldPersistAndQueryProjectsByStatus() {
        Project p1 = Project.builder()
                .name("frontend-monorepo")
                .template(ProjectTemplate.HTML_CSS_JS)
                .language("JavaScript")
                .status(ProjectStatus.ACTIVE)
                .build();
        projectRepository.save(p1);

        Project p2 = Project.builder()
                .name("archived-service")
                .template(ProjectTemplate.BLANK)
                .language("Python")
                .status(ProjectStatus.ARCHIVED)
                .build();
        projectRepository.save(p2);

        List<Project> activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE);
        assertEquals(1, activeProjects.size());
        assertEquals("frontend-monorepo", activeProjects.get(0).getName());
    }

    @Test
    void shouldFindProjectByOwnerId() {
        UUID ownerId = UUID.randomUUID();
        Project p = Project.builder()
                .name("cloud-compiler")
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .ownerId(ownerId)
                .status(ProjectStatus.ACTIVE)
                .build();
        projectRepository.save(p);

        List<Project> owned = projectRepository.findByOwnerId(ownerId);
        assertEquals(1, owned.size());
        assertEquals("cloud-compiler", owned.get(0).getName());
    }

    @Test
    void shouldExcludeDeletedProjects() {
        Project p = Project.builder()
                .name("deleted-app")
                .template(ProjectTemplate.BLANK)
                .language("Text")
                .status(ProjectStatus.DELETED)
                .build();
        Project saved = projectRepository.save(p);

        Optional<Project> found = projectRepository.findByIdAndStatusNot(saved.getId(), ProjectStatus.DELETED);
        assertFalse(found.isPresent());
    }
}
