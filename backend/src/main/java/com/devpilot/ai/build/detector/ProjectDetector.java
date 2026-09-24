package com.devpilot.ai.build.detector;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.entity.ProjectFile;

import java.util.List;

public interface ProjectDetector {
    boolean matches(List<ProjectFile> files);
    ProjectDetectionResponse getDetails(List<ProjectFile> files);
}
