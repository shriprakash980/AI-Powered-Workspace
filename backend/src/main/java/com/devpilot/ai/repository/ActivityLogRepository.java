package com.devpilot.ai.repository;

import com.devpilot.ai.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findByProjectId(UUID projectId);
    List<ActivityLog> findByUserId(UUID userId);
}
