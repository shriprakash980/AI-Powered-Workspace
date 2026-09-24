package com.devpilot.ai.build;

import com.devpilot.ai.build.detector.*;
import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.build.service.BuildDetectionService;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.repository.ProjectFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildDetectionServiceTest {

    @Mock
    private ProjectFileRepository projectFileRepository;

    private BuildDetectionService buildDetectionService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();

        List<ProjectDetector> detectors = List.of(
                new JavaSpringProjectDetector(),
                new JavaMavenProjectDetector(),
                new JavaGradleProjectDetector(),
                new NodeProjectDetector(),
                new PythonProjectDetector(),
                new StaticWebProjectDetector(),
                new DockerProjectDetector()
        );

        buildDetectionService = new BuildDetectionService(projectFileRepository, detectors);
    }

    @Test
    @DisplayName("Should detect Spring Boot project type when pom.xml and Spring Boot starter exist")
    void testDetectSpringBootProject() {
        ProjectFile pom = new ProjectFile();
        pom.setName("pom.xml");
        pom.setContent("<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId></dependency>");

        when(projectFileRepository.findByProjectId(projectId)).thenReturn(List.of(pom));

        ProjectDetectionResponse response = buildDetectionService.detectProjectType(projectId);

        assertNotNull(response);
        assertEquals(ProjectType.JAVA_SPRING, response.primaryType());
    }
}
