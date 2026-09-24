package com.devpilot.ai.build.repository;

import com.devpilot.ai.build.entity.BuildLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BuildLogRepository extends JpaRepository<BuildLog, UUID> {
    List<BuildLog> findByBuildIdOrderBySequenceNumberAsc(UUID buildId);
    long countByBuildId(UUID buildId);
    void deleteByBuildId(UUID buildId);
}
