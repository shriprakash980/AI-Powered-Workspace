package com.devpilot.ai.deployment.repository;

import com.devpilot.ai.deployment.entity.DeploymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, UUID> {
    List<DeploymentLog> findByDeploymentIdOrderBySequenceNumberAsc(UUID deploymentId);
    long countByDeploymentId(UUID deploymentId);
    void deleteByDeploymentId(UUID deploymentId);
}
