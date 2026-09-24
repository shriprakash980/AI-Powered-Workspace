package com.devpilot.ai.cicd.parser;

import com.devpilot.ai.cicd.model.PipelineStepType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class PipelineConfigValidator {

    private static final int MAX_CONFIG_SIZE_BYTES = 64 * 1024; // 64 KB limit
    private static final Set<String> ALLOWED_STEP_TYPES = Set.of(
            "CHECKOUT", "INSTALL", "BUILD", "TEST", "PACKAGE", "ARTIFACT", "IMAGE_BUILD", "DEPLOY", "HEALTH_CHECK"
    );

    public void validateRawYaml(String rawYaml) {
        if (rawYaml == null || rawYaml.isBlank()) {
            throw new IllegalArgumentException("Pipeline configuration file is empty.");
        }
        if (rawYaml.getBytes().length > MAX_CONFIG_SIZE_BYTES) {
            throw new IllegalArgumentException("Pipeline configuration file exceeds maximum allowed size of 64KB.");
        }

        String lower = rawYaml.toLowerCase();
        if (lower.contains("eval(") || lower.contains("exec(") || lower.contains("system(") || lower.contains("cmd.exe") || lower.contains("/bin/sh") || lower.contains("/bin/bash")) {
            throw new IllegalArgumentException("Dangerous shell injection patterns detected in pipeline configuration.");
        }
    }

    public void validateConfig(PipelineConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Invalid pipeline configuration: Configuration object is null.");
        }

        if (config.getName() == null || config.getName().isBlank()) {
            throw new IllegalArgumentException("Invalid pipeline configuration: 'name' field is required.");
        }

        if (config.getSteps() == null || config.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Invalid pipeline configuration: At least one step must be defined.");
        }

        for (PipelineConfig.StepConfig step : config.getSteps()) {
            if (step.getName() == null || step.getName().isBlank()) {
                throw new IllegalArgumentException("Invalid pipeline configuration: Step name is required.");
            }
            if (step.getType() == null || step.getType().isBlank()) {
                throw new IllegalArgumentException("Invalid pipeline configuration: Step type is required.");
            }

            String typeUpper = step.getType().trim().toUpperCase();
            if (!ALLOWED_STEP_TYPES.contains(typeUpper)) {
                throw new IllegalArgumentException("Invalid pipeline configuration: Step type \"" + step.getType() + "\" is not supported. Allowed types are: " + String.join(", ", ALLOWED_STEP_TYPES));
            }
        }
    }
}
