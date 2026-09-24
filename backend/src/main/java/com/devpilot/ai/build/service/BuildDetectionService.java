package com.devpilot.ai.build.service;

import com.devpilot.ai.build.detector.ProjectDetector;
import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.repository.ProjectFileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BuildDetectionService {

    private final ProjectFileRepository projectFileRepository;
    private final List<ProjectDetector> detectors;

    public BuildDetectionService(ProjectFileRepository projectFileRepository, List<ProjectDetector> detectors) {
        this.projectFileRepository = projectFileRepository;
        this.detectors = detectors != null ? detectors : List.of();
    }

    public ProjectDetectionResponse detectProjectType(UUID projectId) {
        List<ProjectFile> files = projectFileRepository != null ? projectFileRepository.findByProjectId(projectId) : List.of();

        List<ProjectType> detected = new ArrayList<>();
        ProjectDetectionResponse primaryDetails = null;

        for (ProjectDetector detector : detectors) {
            if (detector.matches(files)) {
                ProjectDetectionResponse details = detector.getDetails(files);
                detected.addAll(details.detectedTypes());
                if (primaryDetails == null) {
                    primaryDetails = details;
                }
            }
        }

        if (primaryDetails == null) {
            // Default fallback
            return new ProjectDetectionResponse(
                    List.of(ProjectType.STATIC_WEB),
                    ProjectType.STATIC_WEB,
                    "echo 'Default Build'",
                    "echo 'No tests'",
                    "echo 'Start'",
                    ".",
                    null
            );
        }

        return new ProjectDetectionResponse(
                detected.stream().distinct().toList(),
                primaryDetails.primaryType(),
                primaryDetails.suggestedBuildCommand(),
                primaryDetails.suggestedTestCommand(),
                primaryDetails.suggestedStartCommand(),
                primaryDetails.outputDirectory(),
                primaryDetails.dockerfilePath()
        );
    }
}
