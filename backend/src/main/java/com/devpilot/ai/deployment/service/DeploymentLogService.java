package com.devpilot.ai.deployment.service;

import com.devpilot.ai.build.entity.enums.LogType;
import com.devpilot.ai.deployment.dto.DeploymentLogDto;
import com.devpilot.ai.deployment.entity.DeploymentLog;
import com.devpilot.ai.deployment.repository.DeploymentLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DeploymentLogService {

    private final DeploymentLogRepository deploymentLogRepository;
    private final ConcurrentHashMap<UUID, AtomicInteger> sequenceCounters = new ConcurrentHashMap<>();

    public DeploymentLogService(DeploymentLogRepository deploymentLogRepository) {
        this.deploymentLogRepository = deploymentLogRepository;
    }

    @Transactional
    public void appendLog(UUID deploymentId, LogType logType, String message) {
        if (message == null) return;

        AtomicInteger counter = sequenceCounters.computeIfAbsent(deploymentId, k -> {
            long existingCount = deploymentLogRepository.countByDeploymentId(deploymentId);
            return new AtomicInteger((int) existingCount);
        });

        int seq = counter.incrementAndGet();

        DeploymentLog log = new DeploymentLog(null, deploymentId, logType, message, seq, null);
        deploymentLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<DeploymentLogDto> getDeploymentLogs(UUID deploymentId) {
        return deploymentLogRepository.findByDeploymentIdOrderBySequenceNumberAsc(deploymentId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private DeploymentLogDto mapToDto(DeploymentLog log) {
        return new DeploymentLogDto(
                log.getId(),
                log.getDeploymentId(),
                log.getLogType(),
                log.getMessage(),
                log.getSequenceNumber(),
                log.getCreatedAt()
        );
    }
}
