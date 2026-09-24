package com.devpilot.ai.build.service;

import com.devpilot.ai.build.dto.BuildLogDto;
import com.devpilot.ai.build.entity.BuildLog;
import com.devpilot.ai.build.entity.enums.LogType;
import com.devpilot.ai.build.repository.BuildLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BuildLogService {

    private final BuildLogRepository buildLogRepository;
    private final ConcurrentHashMap<UUID, AtomicInteger> sequenceCounters = new ConcurrentHashMap<>();
    private static final int MAX_LOG_LINES = 1000;

    public BuildLogService(BuildLogRepository buildLogRepository) {
        this.buildLogRepository = buildLogRepository;
    }

    @Transactional
    public void appendLog(UUID buildId, LogType logType, String message) {
        if (message == null) return;

        AtomicInteger counter = sequenceCounters.computeIfAbsent(buildId, k -> {
            long existingCount = buildLogRepository.countByBuildId(buildId);
            return new AtomicInteger((int) existingCount);
        });

        int seq = counter.incrementAndGet();
        if (seq > MAX_LOG_LINES) {
            if (seq == MAX_LOG_LINES + 1) {
                BuildLog overflowLog = new BuildLog(null, buildId, LogType.WARNING, "[BuildLogService] Log buffer size limit reached. Additional lines truncated.", seq, null);
                buildLogRepository.save(overflowLog);
            }
            return;
        }

        BuildLog log = new BuildLog(null, buildId, logType, message, seq, null);
        buildLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<BuildLogDto> getBuildLogs(UUID buildId) {
        return buildLogRepository.findByBuildIdOrderBySequenceNumberAsc(buildId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private BuildLogDto mapToDto(BuildLog log) {
        return new BuildLogDto(
                log.getId(),
                log.getBuildId(),
                log.getLogType(),
                log.getMessage(),
                log.getSequenceNumber(),
                log.getCreatedAt()
        );
    }
}
