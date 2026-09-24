package com.devpilot.ai.service;

import com.devpilot.ai.entity.ActivityLog;
import com.devpilot.ai.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void logActivity(UUID userId, UUID projectId, String action, String metadata) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .userId(userId)
                    .projectId(projectId)
                    .action(action)
                    .metadata(metadata)
                    .createdAt(Instant.now())
                    .build();
            activityLogRepository.save(activityLog);
            log.debug("Activity recorded: action='{}', userId={}, projectId={}", action, userId, projectId);
        } catch (Exception e) {
            log.error("Failed to record activity log for action '{}': {}", action, e.getMessage());
        }
    }
}
