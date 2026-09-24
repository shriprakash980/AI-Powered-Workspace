package com.devpilot.ai.git.repository;

import com.devpilot.ai.git.entity.GitHubConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {
    Optional<GitHubConnection> findByUserIdAndRevokedAtIsNull(UUID userId);
    Optional<GitHubConnection> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}
