package com.devpilot.ai.repository;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProjectFileRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectFileRepository projectFileRepository;

    @Test
    void shouldPersistFileAndQueryByProject() {
        Project project = projectRepository.save(Project.builder()
                .name("sample-workspace")
                .template(ProjectTemplate.HTML_CSS_JS)
                .language("JavaScript")
                .status(ProjectStatus.ACTIVE)
                .build());

        ProjectFile rootFile = projectFileRepository.save(ProjectFile.builder()
                .projectId(project.getId())
                .name("index.html")
                .path("/index.html")
                .fileType("html")
                .content("<!DOCTYPE html><html></html>")
                .isDirectory(false)
                .build());

        assertNotNull(rootFile.getId());

        List<ProjectFile> files = projectFileRepository.findByProjectId(project.getId());
        assertEquals(1, files.size());
        assertEquals("/index.html", files.get(0).getPath());
    }

    @Test
    void shouldEnforceUniquePathPerProject() {
        Project project = projectRepository.save(Project.builder()
                .name("unique-path-test")
                .template(ProjectTemplate.BLANK)
                .language("Text")
                .status(ProjectStatus.ACTIVE)
                .build());

        ProjectFile file1 = ProjectFile.builder()
                .projectId(project.getId())
                .name("README.md")
                .path("/README.md")
                .content("# Readme")
                .build();
        projectFileRepository.saveAndFlush(file1);

        ProjectFile file2 = ProjectFile.builder()
                .projectId(project.getId())
                .name("README.md")
                .path("/README.md")
                .content("# Readme Duplicate")
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            projectFileRepository.saveAndFlush(file2);
        });
    }
}
