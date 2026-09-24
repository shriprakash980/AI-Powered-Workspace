package com.devpilot.ai.build.detector;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PythonProjectDetector implements ProjectDetector {

    @Override
    public boolean matches(List<ProjectFile> files) {
        return files.stream().anyMatch(f ->
                "requirements.txt".equalsIgnoreCase(f.getName()) ||
                "setup.py".equalsIgnoreCase(f.getName()) ||
                "pyproject.toml".equalsIgnoreCase(f.getName()) ||
                (f.getName() != null && f.getName().endsWith(".py"))
        );
    }

    @Override
    public ProjectDetectionResponse getDetails(List<ProjectFile> files) {
        return new ProjectDetectionResponse(
                List.of(ProjectType.PYTHON),
                ProjectType.PYTHON,
                "python -m compileall .",
                "pytest",
                "python app.py",
                ".",
                files.stream().anyMatch(f -> "Dockerfile".equalsIgnoreCase(f.getName())) ? "Dockerfile" : null
        );
    }
}
