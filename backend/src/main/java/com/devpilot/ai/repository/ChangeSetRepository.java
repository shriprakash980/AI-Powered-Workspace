package com.devpilot.ai.repository;

import com.devpilot.ai.entity.ChangeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeSetRepository extends JpaRepository<ChangeSet, UUID> {

    List<ChangeSet> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ChangeSet> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<ChangeSet> findByProjectIdAndId(UUID projectId, UUID id);
}
