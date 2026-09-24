package com.devpilot.ai.repository;

import com.devpilot.ai.entity.FileChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileChangeRepository extends JpaRepository<FileChange, UUID> {

    List<FileChange> findByChangeSetIdOrderByCreatedAtAsc(UUID changeSetId);
}
