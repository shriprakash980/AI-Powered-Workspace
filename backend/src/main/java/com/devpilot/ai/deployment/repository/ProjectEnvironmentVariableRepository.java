package com.devpilot.ai.deployment.repository;

import com.devpilot.ai.deployment.entity.ProjectEnvironmentVariable;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectEnvironmentVariableRepository extends JpaRepository<ProjectEnvironmentVariable, UUID> {
    List<ProjectEnvironmentVariable> findByProjectIdAndEnvironment(UUID projectId, DeploymentEnvironment environment);
    List<ProjectEnvironmentVariable> findByProjectId(UUID projectId);
    Optional<ProjectEnvironmentVariable> findByProjectIdAndNameAndEnvironment(UUID projectId, String name, DeploymentEnvironment environment);
}
