package com.devpilot.ai.build.service;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.ChatMessage;
import com.devpilot.ai.ai.provider.AIProvider;
import com.devpilot.ai.ai.provider.AIProviderFactory;
import com.devpilot.ai.build.dto.BuildDiagnosisResponse;
import com.devpilot.ai.build.dto.BuildLogDto;
import com.devpilot.ai.build.entity.enums.LogType;
import com.devpilot.ai.entity.enums.AIProviderType;
import com.devpilot.ai.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BuildAiService {

    private static final Logger log = LoggerFactory.getLogger(BuildAiService.class);

    private final BuildLogService buildLogService;
    private final AIProviderFactory aiProviderFactory;
    private final ProjectService projectService;

    public BuildAiService(BuildLogService buildLogService, AIProviderFactory aiProviderFactory, ProjectService projectService) {
        this.buildLogService = buildLogService;
        this.aiProviderFactory = aiProviderFactory;
        this.projectService = projectService;
    }

    public BuildDiagnosisResponse diagnoseBuildFailure(UUID projectId, UUID buildId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);

        List<BuildLogDto> logs = buildLogService.getBuildLogs(buildId);
        String recentLogs = logs.stream()
                .filter(l -> l.logType() == LogType.STDERR || l.logType() == LogType.ERROR)
                .map(l -> "[" + l.logType() + "] " + l.message())
                .collect(Collectors.joining("\n"));

        if (recentLogs.isBlank()) {
            recentLogs = logs.stream()
                    .map(l -> "[" + l.logType() + "] " + l.message())
                    .collect(Collectors.joining("\n"));
        }

        String prompt = "Diagnose the following build failure log and suggest a resolution:\n\n" + recentLogs;

        try {
            AIProvider provider = aiProviderFactory.getProvider(AIProviderType.OPENAI.name());
            AIRequest request = new AIRequest(provider.getDefaultModel(), List.of(new ChatMessage("user", prompt)), 0.2, 2048, false);
            AIResponse response = provider.generate(request);
            String aiResult = response != null ? response.getContent() : "No diagnosis generated";

            return new BuildDiagnosisResponse(
                    buildId,
                    "Compilation or dependency execution error detected in build runner.",
                    aiResult
            );
        } catch (Exception e) {
            log.warn("AI build diagnosis warning: {}", e.getMessage());
            return new BuildDiagnosisResponse(
                    buildId,
                    "Build script exited with non-zero exit code.",
                    "Inspect dependencies, command parameters, and missing source imports."
            );
        }
    }
}
