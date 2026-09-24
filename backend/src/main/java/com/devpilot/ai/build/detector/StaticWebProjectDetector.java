package com.devpilot.ai.build.detector;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaticWebProjectDetector implements ProjectDetector {

    @Override
    public boolean matches(List<ProjectFile> files) {
        return files.stream().anyMatch(f -> "index.html".equalsIgnoreCase(f.getName()));
    }

    @Override
    public ProjectDetectionResponse getDetails(List<ProjectFile> files) {
        return new ProjectDetectionResponse(
                List.of(ProjectType.STATIC_WEB),
                ProjectType.STATIC_WEB,
                "echo 'Static Web Ready'",
                "echo 'No unit tests configured for static web'",
                "npx serve .",
                ".",
                files.stream().anyMatch(f -> "Dockerfile".equalsIgnoreCase(f.getName())) ? "Dockerfile" : null
        );
    }
}
