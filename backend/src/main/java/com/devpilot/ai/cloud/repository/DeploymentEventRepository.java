package com.devpilot.ai.cloud.repository;

import com.devpilot.ai.cloud.entity.DeploymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentEventRepository extends JpaRepository<DeploymentEvent, UUID> {
    List<DeploymentEvent> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<DeploymentEvent> findByDeploymentIdOrderByCreatedAtAsc(UUID deploymentId);
}
