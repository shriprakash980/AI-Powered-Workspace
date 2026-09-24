package com.devpilot.ai.cicd.service;

import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.build.service.BuildDetectionService;
import com.devpilot.ai.cicd.parser.PipelineConfig;
import com.devpilot.ai.cicd.parser.PipelineConfigParser;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.repository.ProjectFileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PipelineConfigService {

    private static final Logger log = LoggerFactory.getLogger(PipelineConfigService.class);

    private final ProjectFileRepository projectFileRepository;
    private final PipelineConfigParser configParser;
    private final BuildDetectionService buildDetectionService;

    public PipelineConfigService(
            ProjectFileRepository projectFileRepository,
            PipelineConfigParser configParser,
            BuildDetectionService buildDetectionService) {
        this.projectFileRepository = projectFileRepository;
        this.configParser = configParser;
        this.buildDetectionService = buildDetectionService;
    }

    public String loadRawConfig(UUID projectId, String configPath) {
        String targetPath = (configPath == null || configPath.isBlank()) ? "devpilot-ci.yml" : configPath;
        if (!targetPath.startsWith("/")) {
            targetPath = "/" + targetPath;
        }

        Optional<ProjectFile> fileOpt = projectFileRepository.findByProjectIdAndPath(projectId, targetPath);
        if (fileOpt.isPresent() && fileOpt.get().getContent() != null && !fileOpt.get().getContent().isBlank()) {
            return fileOpt.get().getContent();
        }

        return generateDefaultConfigYaml(projectId);
    }

    public PipelineConfig parseConfig(UUID projectId, String configPath) {
        String rawYaml = loadRawConfig(projectId, configPath);
        return configParser.parseAndValidate(rawYaml);
    }

    public String generateDefaultConfigYaml(UUID projectId) {
        ProjectDetectionResponse detection = buildDetectionService.detectProjectType(projectId);
        ProjectType primary = detection.primaryType();

        if (primary == ProjectType.NODEJS) {
            return """
                    name: Node Application
                    triggers:
                      push: true
                      pull_request: true
                      manual: true
                    environment:
                      node: "20"
                    steps:
                      - name: Install
                        type: INSTALL
                      - name: Build
                        type: BUILD
                      - name: Test
                        type: TEST
                      - name: Package
                        type: PACKAGE
                    artifact:
                      enabled: true
                    deployment:
                      enabled: false
                      environment: development
                    """;
        } else if (primary == ProjectType.PYTHON) {
            return """
                    name: Python Application
                    triggers:
                      push: true
                      pull_request: true
                      manual: true
                    environment:
                      python: "3.11"
                    steps:
                      - name: Install
                        type: INSTALL
                      - name: Test
                        type: TEST
                      - name: Build
                        type: BUILD
                    artifact:
                      enabled: true
                    deployment:
                      enabled: false
                      environment: development
                    """;
        } else {
            return """
                    name: Java Application
                    triggers:
                      push: true
                      pull_request: true
                      manual: true
                    environment:
                      java: "21"
                    steps:
                      - name: Build
                        type: BUILD
                      - name: Test
                        type: TEST
                      - name: Package
                        type: PACKAGE
                    artifact:
                      enabled: true
                    deployment:
                      enabled: false
                      environment: development
                    """;
        }
    }
}
