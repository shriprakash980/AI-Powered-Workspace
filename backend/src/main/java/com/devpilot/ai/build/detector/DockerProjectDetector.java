package com.devpilot.ai.build.detector;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DockerProjectDetector implements ProjectDetector {

    @Override
    public boolean matches(List<ProjectFile> files) {
        return files.stream().anyMatch(f -> "Dockerfile".equalsIgnoreCase(f.getName()));
    }

    @Override
    public ProjectDetectionResponse getDetails(List<ProjectFile> files) {
        return new ProjectDetectionResponse(
                List.of(ProjectType.DOCKER),
                ProjectType.DOCKER,
                "docker build -t app .",
                "docker run --rm app test",
                "docker run -p 8080:8080 app",
                ".",
                "Dockerfile"
        );
    }
}
