package com.devpilot.ai.build.detector;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaGradleProjectDetector implements ProjectDetector {

    @Override
    public boolean matches(List<ProjectFile> files) {
        return files.stream().anyMatch(f -> "build.gradle".equalsIgnoreCase(f.getName()) || "build.gradle.kts".equalsIgnoreCase(f.getName()));
    }

    @Override
    public ProjectDetectionResponse getDetails(List<ProjectFile> files) {
        return new ProjectDetectionResponse(
                List.of(ProjectType.JAVA_GRADLE),
                ProjectType.JAVA_GRADLE,
                "gradle build -x test",
                "gradle test",
                "java -jar build/libs/*.jar",
                "build/libs",
                files.stream().anyMatch(f -> "Dockerfile".equalsIgnoreCase(f.getName())) ? "Dockerfile" : null
        );
    }
}
