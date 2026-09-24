package com.devpilot.ai.repository;

import com.devpilot.ai.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<Conversation> findByUserIdAndProjectIdOrderByUpdatedAtDesc(UUID userId, UUID projectId);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
