package com.devpilot.ai.deployment.repository;

import com.devpilot.ai.deployment.entity.Deployment;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProjectId(UUID projectId);
    Page<Deployment> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
    List<Deployment> findByProjectIdAndEnvironmentOrderByCreatedAtDesc(UUID projectId, DeploymentEnvironment environment);
    List<Deployment> findByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, DeploymentStatus status);
    Optional<Deployment> findFirstByProjectIdAndEnvironmentAndStatusOrderByCreatedAtDesc(
            UUID projectId, DeploymentEnvironment environment, DeploymentStatus status);
    List<Deployment> findByStatus(DeploymentStatus status);
    long countByUserIdAndStatus(UUID userId, DeploymentStatus status);
    long countByProjectId(UUID projectId);
}
