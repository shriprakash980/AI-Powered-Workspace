package com.devpilot.ai.repository;

import com.devpilot.ai.entity.AIRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AIRequestLogRepository extends JpaRepository<AIRequestLog, UUID> {

    List<AIRequestLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AIRequestLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
