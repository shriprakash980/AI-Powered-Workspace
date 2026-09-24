package com.devpilot.ai.repository;

import com.devpilot.ai.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, UUID> {
    List<ProjectFile> findByProjectId(UUID projectId);
    List<ProjectFile> findByProjectIdAndParentId(UUID projectId, UUID parentId);
    Optional<ProjectFile> findByProjectIdAndPath(UUID projectId, String path);
    boolean existsByProjectIdAndPath(UUID projectId, String path);
}
