package com.devpilot.ai.cloud.repository;

import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.model.CloudProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentTargetRepository extends JpaRepository<DeploymentTarget, UUID> {
    List<DeploymentTarget> findByProjectId(UUID projectId);
    Optional<DeploymentTarget> findByIdAndProjectId(UUID id, UUID projectId);
    Optional<DeploymentTarget> findFirstByProjectIdAndProvider(UUID projectId, CloudProviderType provider);
}
