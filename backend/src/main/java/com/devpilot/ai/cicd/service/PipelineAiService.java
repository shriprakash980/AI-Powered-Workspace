package com.devpilot.ai.cicd.service;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.ChatMessage;
import com.devpilot.ai.ai.provider.AIProvider;
import com.devpilot.ai.ai.provider.AIProviderFactory;
import com.devpilot.ai.cicd.dto.PipelineDiagnosisResponse;
import com.devpilot.ai.cicd.dto.PipelineLogDto;
import com.devpilot.ai.cicd.dto.PipelineLogResponse;
import com.devpilot.ai.cicd.entity.PipelineRun;
import com.devpilot.ai.cicd.entity.PipelineStep;
import com.devpilot.ai.cicd.model.PipelineStepStatus;
import com.devpilot.ai.cicd.repository.PipelineRunRepository;
import com.devpilot.ai.cicd.repository.PipelineStepRepository;
import com.devpilot.ai.entity.enums.AIProviderType;
import com.devpilot.ai.service.ProjectService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PipelineAiService {

    private static final Logger log = LoggerFactory.getLogger(PipelineAiService.class);

    private final PipelineRunRepository runRepository;
    private final PipelineStepRepository stepRepository;
    private final PipelineLogService logService;
    private final AIProviderFactory aiProviderFactory;
    private final ProjectService projectService;

    public PipelineAiService(
            PipelineRunRepository runRepository,
            PipelineStepRepository stepRepository,
            PipelineLogService logService,
            AIProviderFactory aiProviderFactory,
            ProjectService projectService) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.logService = logService;
        this.aiProviderFactory = aiProviderFactory;
        this.projectService = projectService;
    }

    public PipelineDiagnosisResponse diagnosePipelineFailure(UUID projectId, UUID runId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);

        List<PipelineStep> steps = stepRepository.findByPipelineRunIdOrderByStepOrderAsc(runId);
        PipelineStep failedStep = steps.stream()
                .filter(s -> s.getStatus() == PipelineStepStatus.FAILED)
                .findFirst()
                .orElse(null);

        String stepName = failedStep != null ? failedStep.getStepName() : "BUILD";

        PipelineLogResponse logRes = logService.getRunLogs(runId, stepName);
        String recentLogs = logRes.logs().stream()
                .filter(l -> "stderr".equalsIgnoreCase(l.stream()) || l.message().contains("ERROR") || l.message().contains("FAILED"))
                .map(l -> "[" + l.stream() + "] " + l.message())
                .collect(Collectors.joining("\n"));

        if (recentLogs.isBlank() && !logRes.logs().isEmpty()) {
            recentLogs = logRes.logs().stream()
                    .map(l -> "[" + l.stream() + "] " + l.message())
                    .collect(Collectors.joining("\n"));
        }

        String prompt = "Diagnose the following CI/CD pipeline step failure (" + stepName + ") and provide the root cause and a suggested fix:\n\n" + recentLogs;

        try {
            AIProvider provider = aiProviderFactory.getProvider(AIProviderType.OPENAI.name());
            AIRequest request = new AIRequest(provider.getDefaultModel(), List.of(new ChatMessage("user", prompt)), 0.2, 2048, false);
            AIResponse response = provider.generate(request);
            String aiResult = response != null ? response.getContent() : "No diagnosis generated";

            return new PipelineDiagnosisResponse(
                    runId,
                    stepName,
                    "Step " + stepName + " failed execution.",
                    "Compilation, dependency installation, or automated test assertion failed.",
                    "devpilot-ci.yml",
                    aiResult
            );
        } catch (Exception e) {
            log.warn("AI pipeline diagnosis warning: {}", e.getMessage());
            return new PipelineDiagnosisResponse(
                    runId,
                    stepName,
                    "Pipeline step " + stepName + " exited with non-zero exit code.",
                    "Script execution error in build runner.",
                    "devpilot-ci.yml",
                    "Inspect step logs, dependencies, and configuration parameters."
            );
        }
    }
}
