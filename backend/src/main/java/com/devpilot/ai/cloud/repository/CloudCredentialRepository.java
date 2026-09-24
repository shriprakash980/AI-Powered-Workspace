package com.devpilot.ai.cloud.repository;

import com.devpilot.ai.cloud.entity.CloudCredential;
import com.devpilot.ai.cloud.model.CloudProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloudCredentialRepository extends JpaRepository<CloudCredential, UUID> {
    List<CloudCredential> findByUserId(UUID userId);
    Optional<CloudCredential> findByUserIdAndProvider(UUID userId, CloudProviderType provider);
}
