package com.devpilot.ai.deployment.service;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.ChatMessage;
import com.devpilot.ai.ai.provider.AIProvider;
import com.devpilot.ai.ai.provider.AIProviderFactory;
import com.devpilot.ai.build.entity.enums.LogType;
import com.devpilot.ai.deployment.dto.DeploymentDiagnosisResponse;
import com.devpilot.ai.deployment.dto.DeploymentLogDto;
import com.devpilot.ai.entity.enums.AIProviderType;
import com.devpilot.ai.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeploymentAiService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentAiService.class);

    private final DeploymentLogService deploymentLogService;
    private final AIProviderFactory aiProviderFactory;
    private final ProjectService projectService;

    public DeploymentAiService(DeploymentLogService deploymentLogService, AIProviderFactory aiProviderFactory, ProjectService projectService) {
        this.deploymentLogService = deploymentLogService;
        this.aiProviderFactory = aiProviderFactory;
        this.projectService = projectService;
    }

    public DeploymentDiagnosisResponse diagnoseDeploymentFailure(UUID projectId, UUID deploymentId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);

        List<DeploymentLogDto> logs = deploymentLogService.getDeploymentLogs(deploymentId);
        String recentLogs = logs.stream()
                .filter(l -> l.logType() == LogType.STDERR || l.logType() == LogType.ERROR)
                .map(l -> "[" + l.logType() + "] " + l.message())
                .collect(Collectors.joining("\n"));

        if (recentLogs.isBlank()) {
            recentLogs = logs.stream()
                    .map(l -> "[" + l.logType() + "] " + l.message())
                    .collect(Collectors.joining("\n"));
        }

        String prompt = "Diagnose the following container deployment failure logs and provide the root cause and a suggested fix:\n\n" + recentLogs;

        try {
            AIProvider provider = aiProviderFactory.getProvider(AIProviderType.OPENAI.name());
            AIRequest request = new AIRequest(provider.getDefaultModel(), List.of(new ChatMessage("user", prompt)), 0.2, 2048, false);
            AIResponse response = provider.generate(request);
            String aiResult = response != null ? response.getContent() : "No diagnosis generated";

            return new DeploymentDiagnosisResponse(
                    deploymentId,
                    "Deployment container exited with runtime error or failed health check.",
                    aiResult
            );
        } catch (Exception e) {
            log.warn("AI deployment diagnosis warning: {}", e.getMessage());
            return new DeploymentDiagnosisResponse(
                    deploymentId,
                    "Container deployment failed to pass health check on allocated port.",
                    "Ensure application listens on host port binding and environment variable secrets are configured correctly."
            );
        }
    }
}
