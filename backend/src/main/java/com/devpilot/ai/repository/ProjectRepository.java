package com.devpilot.ai.repository;

import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByOwnerId(UUID ownerId);
    List<Project> findByOwnerIdAndStatusNot(UUID ownerId, ProjectStatus status);
    Optional<Project> findByIdAndStatusNot(UUID id, ProjectStatus status);
}
