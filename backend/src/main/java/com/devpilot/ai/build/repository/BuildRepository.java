package com.devpilot.ai.build.repository;

import com.devpilot.ai.build.entity.Build;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildRepository extends JpaRepository<Build, UUID> {
    Page<Build> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
    List<Build> findByProjectIdAndStatus(UUID projectId, BuildStatus status);
    Optional<Build> findFirstByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, BuildStatus status);
    long countByUserIdAndStatus(UUID userId, BuildStatus status);
}
