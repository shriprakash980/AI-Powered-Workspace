package com.devpilot.ai.build.detector;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaSpringProjectDetector implements ProjectDetector {

    @Override
    public boolean matches(List<ProjectFile> files) {
        boolean hasPom = files.stream().anyMatch(f -> "pom.xml".equalsIgnoreCase(f.getName()));
        boolean hasSpringBootContent = files.stream()
                .filter(f -> f.getContent() != null)
                .anyMatch(f -> f.getContent().contains("spring-boot") || f.getContent().contains("@SpringBootApplication"));
        return hasPom && hasSpringBootContent;
    }

    @Override
    public ProjectDetectionResponse getDetails(List<ProjectFile> files) {
        return new ProjectDetectionResponse(
                List.of(ProjectType.JAVA_SPRING, ProjectType.JAVA_MAVEN),
                ProjectType.JAVA_SPRING,
                "mvn compile",
                "mvn test",
                "java -jar target/*.jar",
                "target",
                files.stream().anyMatch(f -> "Dockerfile".equalsIgnoreCase(f.getName())) ? "Dockerfile" : null
        );
    }
}
