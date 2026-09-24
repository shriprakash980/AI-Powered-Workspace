package com.devpilot.ai.deployment.service;

import com.devpilot.ai.deployment.dto.EnvironmentVariableRequest;
import com.devpilot.ai.deployment.dto.EnvironmentVariableResponse;
import com.devpilot.ai.deployment.entity.ProjectEnvironmentVariable;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.repository.ProjectEnvironmentVariableRepository;
import com.devpilot.ai.git.security.TokenEncryptionService;
import com.devpilot.ai.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class EnvironmentVariableService {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentVariableService.class);

    private final ProjectEnvironmentVariableRepository envVarRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final ProjectService projectService;

    private static final Set<String> BLOCKED_SYSTEM_KEYS = Set.of(
            "DATABASE_PASSWORD", "SPRING_DATASOURCE_PASSWORD", "JWT_SECRET",
            "OPENAI_API_KEY", "GITHUB_CLIENT_SECRET", "ANTHROPIC_API_KEY", "GEMINI_API_KEY"
    );

    public EnvironmentVariableService(ProjectEnvironmentVariableRepository envVarRepository, TokenEncryptionService tokenEncryptionService, ProjectService projectService) {
        this.envVarRepository = envVarRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.projectService = projectService;
    }

    @Transactional
    public EnvironmentVariableResponse saveVariable(UUID projectId, UUID userId, EnvironmentVariableRequest request) {
        projectService.verifyProjectOwnership(projectId, userId);

        if (BLOCKED_SYSTEM_KEYS.contains(request.name().toUpperCase())) {
            throw new IllegalArgumentException("Cannot use reserved system secret name: " + request.name());
        }

        String encrypted = tokenEncryptionService.encrypt(request.value());

        Optional<ProjectEnvironmentVariable> existingOpt = envVarRepository
                .findByProjectIdAndNameAndEnvironment(projectId, request.name(), request.environment());

        ProjectEnvironmentVariable var;
        if (existingOpt.isPresent()) {
            var = existingOpt.get();
            var.setEncryptedValue(encrypted);
            var.setUpdatedAt(Instant.now());
        } else {
            var = ProjectEnvironmentVariable.builder()
                    .projectId(projectId)
                    .name(request.name())
                    .encryptedValue(encrypted)
                    .environment(request.environment())
                    .build();
        }

        ProjectEnvironmentVariable saved = envVarRepository.save(var);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentVariableResponse> getProjectVariables(UUID projectId, UUID userId, DeploymentEnvironment environment) {
        projectService.verifyProjectOwnership(projectId, userId);

        List<ProjectEnvironmentVariable> vars = environment != null
                ? envVarRepository.findByProjectIdAndEnvironment(projectId, environment)
                : envVarRepository.findByProjectId(projectId);

        return vars.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public void deleteVariable(UUID projectId, UUID variableId, UUID userId) {
        projectService.verifyProjectOwnership(projectId, userId);
        envVarRepository.deleteById(variableId);
        log.info("Deleted environment variable ID: {} for project: {}", variableId, projectId);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getDecryptedVariablesMap(UUID projectId, DeploymentEnvironment environment) {
        List<ProjectEnvironmentVariable> vars = envVarRepository.findByProjectIdAndEnvironment(projectId, environment);
        Map<String, String> decryptedMap = new HashMap<>();

        for (ProjectEnvironmentVariable v : vars) {
            try {
                String decrypted = tokenEncryptionService.decrypt(v.getEncryptedValue());
                decryptedMap.put(v.getName(), decrypted);
            } catch (Exception e) {
                log.warn("Failed to decrypt environment variable {} for project {}", v.getName(), projectId);
            }
        }
        return decryptedMap;
    }

    private EnvironmentVariableResponse mapToResponse(ProjectEnvironmentVariable v) {
        return new EnvironmentVariableResponse(
                v.getId(),
                v.getProjectId(),
                v.getName(),
                "••••••••", // Masked value
                v.getEnvironment(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}
