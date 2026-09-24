package com.devpilot.ai.cicd.repository;

import com.devpilot.ai.cicd.entity.PipelineRun;
import com.devpilot.ai.cicd.model.PipelineRunStatus;
import com.devpilot.ai.cicd.model.PipelineTriggerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineRunRepository extends JpaRepository<PipelineRun, UUID> {
    Page<PipelineRun> findByPipelineIdOrderByCreatedAtDesc(UUID pipelineId, Pageable pageable);
    Page<PipelineRun> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
    long countByPipelineIdAndStatus(UUID pipelineId, PipelineRunStatus status);
    long countByProjectIdAndStatus(UUID projectId, PipelineRunStatus status);
    Optional<PipelineRun> findFirstByPipelineIdAndCommitShaAndTriggerTypeAndStatus(
            UUID pipelineId, String commitSha, PipelineTriggerType triggerType, PipelineRunStatus status);
    Optional<PipelineRun> findByDeliveryId(String deliveryId);
    List<PipelineRun> findByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, PipelineRunStatus status);
}
