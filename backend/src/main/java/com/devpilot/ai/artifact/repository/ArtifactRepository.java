package com.devpilot.ai.artifact.repository;

import com.devpilot.ai.artifact.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {
    List<Artifact> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Artifact> findByBuildId(UUID buildId);
    Optional<Artifact> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
