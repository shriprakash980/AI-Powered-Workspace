package com.devpilot.ai.cicd.service;

import com.devpilot.ai.cicd.dto.PipelineLogDto;
import com.devpilot.ai.cicd.dto.PipelineLogResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

@Service
public class PipelineLogService {

    private static final Logger log = LoggerFactory.getLogger(PipelineLogService.class);

    private static final Pattern SECRET_KEY_VALUE_PATTERN = Pattern.compile("(?i)(secret|token|password|api_key|access_token|authorization)\\s*[:=]\\s*[\"']?([^\"'\\s]+)[\"']?");
    private static final Pattern GITHUB_TOKEN_PATTERN = Pattern.compile("(ghp_[A-Za-z0-9_]{30,})");
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)Bearer\\s+([A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+)");

    private final Map<UUID, List<PipelineLogDto>> logsByRun = new ConcurrentHashMap<>();
    private final long maxLogSizeBytes;

    public PipelineLogService(@Value("${app.cicd.max-log-size-mb:5}") long maxLogSizeMb) {
        this.maxLogSizeBytes = maxLogSizeMb * 1024 * 1024;
    }

    public void appendLog(UUID runId, String stepName, String stream, String message) {
        if (runId == null || message == null) return;

        String sanitizedMessage = sanitizeLogSecrets(message);
        String timestamp = Instant.now().toString();
        PipelineLogDto logDto = new PipelineLogDto(timestamp, stepName != null ? stepName : "GENERAL", stream != null ? stream : "stdout", sanitizedMessage);

        List<PipelineLogDto> logs = logsByRun.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>());
        
        // Truncation safeguard
        if (logs.size() > 5000) {
            logs.remove(0);
        }
        logs.add(logDto);
        log.debug("[PipelineRun:{}] [{}] {}", runId, logDto.stream(), sanitizedMessage);
    }

    public PipelineLogResponse getRunLogs(UUID runId, String stepName) {
        List<PipelineLogDto> logs = logsByRun.getOrDefault(runId, List.of());
        if (stepName != null && !stepName.isBlank()) {
            logs = logs.stream()
                    .filter(l -> stepName.equalsIgnoreCase(l.stepName()))
                    .toList();
        }
        return new PipelineLogResponse(runId, stepName, logs);
    }

    public String sanitizeLogSecrets(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return rawMessage;
        }
        String masked = rawMessage;
        masked = GITHUB_TOKEN_PATTERN.matcher(masked).replaceAll("ghp_********");
        masked = BEARER_TOKEN_PATTERN.matcher(masked).replaceAll("Bearer ********");
        masked = SECRET_KEY_VALUE_PATTERN.matcher(masked).replaceAll("$1=********");
        return masked;
    }

    public void clearRunLogs(UUID runId) {
        logsByRun.remove(runId);
    }
}
