package com.devpilot.ai.cicd.repository;

import com.devpilot.ai.cicd.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {
    List<Pipeline> findByProjectId(UUID projectId);
    Optional<Pipeline> findByIdAndProjectId(UUID id, UUID projectId);
    Optional<Pipeline> findFirstByProjectIdAndEnabledTrue(UUID projectId);
}
