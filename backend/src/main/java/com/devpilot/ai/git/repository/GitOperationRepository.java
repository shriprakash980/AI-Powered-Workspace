package com.devpilot.ai.git.repository;

import com.devpilot.ai.git.entity.GitOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GitOperationRepository extends JpaRepository<GitOperation, UUID> {
    List<GitOperation> findTop20ByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Page<GitOperation> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
}
