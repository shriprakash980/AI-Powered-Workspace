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

    Optional<ProjectFile> findByProjectIdAndId(UUID projectId, UUID id);

    Optional<ProjectFile> findByProjectIdAndPath(UUID projectId, String path);

    boolean existsByProjectIdAndParentIdAndName(UUID projectId, UUID parentId, String name);

    boolean existsByProjectIdAndPath(UUID projectId, String path);

    List<ProjectFile> findByProjectIdAndPathStartingWith(UUID projectId, String pathPrefix);

    List<ProjectFile> findByProjectIdAndNameContainingIgnoreCaseOrProjectIdAndPathContainingIgnoreCase(
            UUID projectId1, String name, UUID projectId2, String path);

    long countByProjectId(UUID projectId);

    List<ProjectFile> findTop10ByProjectIdAndIsDirectoryFalseOrderByUpdatedAtDesc(UUID projectId);
}
