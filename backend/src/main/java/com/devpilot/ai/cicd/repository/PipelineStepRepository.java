package com.devpilot.ai.cicd.repository;

import com.devpilot.ai.cicd.entity.PipelineStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PipelineStepRepository extends JpaRepository<PipelineStep, UUID> {
    List<PipelineStep> findByPipelineRunIdOrderByStepOrderAsc(UUID pipelineRunId);
}
