package com.devpilot.ai.build.detector;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaMavenProjectDetector implements ProjectDetector {

    @Override
    public boolean matches(List<ProjectFile> files) {
        return files.stream().anyMatch(f -> "pom.xml".equalsIgnoreCase(f.getName()));
    }

    @Override
    public ProjectDetectionResponse getDetails(List<ProjectFile> files) {
        return new ProjectDetectionResponse(
                List.of(ProjectType.JAVA_MAVEN),
                ProjectType.JAVA_MAVEN,
                "mvn package -DskipTests",
                "mvn test",
                "java -jar target/*.jar",
                "target",
                files.stream().anyMatch(f -> "Dockerfile".equalsIgnoreCase(f.getName())) ? "Dockerfile" : null
        );
    }
}
