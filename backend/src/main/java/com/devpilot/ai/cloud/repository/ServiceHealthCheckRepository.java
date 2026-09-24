package com.devpilot.ai.cloud.repository;

import com.devpilot.ai.cloud.entity.ServiceHealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceHealthCheckRepository extends JpaRepository<ServiceHealthCheck, UUID> {
    List<ServiceHealthCheck> findByProjectIdOrderByCheckedAtDesc(UUID projectId);
    List<ServiceHealthCheck> findByDeploymentIdOrderByCheckedAtDesc(UUID deploymentId);
    Optional<ServiceHealthCheck> findFirstByDeploymentIdOrderByCheckedAtDesc(UUID deploymentId);
}
