package com.devpilot.ai.service;

import com.devpilot.ai.dto.ai.ConversationDetailResponse;
import com.devpilot.ai.dto.ai.ConversationMessageResponse;
import com.devpilot.ai.dto.ai.ConversationResponse;
import com.devpilot.ai.entity.Conversation;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ConversationMessageRepository;
import com.devpilot.ai.repository.ConversationRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ProjectRepository projectRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMessageRepository messageRepository,
                               ProjectRepository projectRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(UserPrincipal userPrincipal, UUID projectId) {
        List<Conversation> conversations;
        if (projectId != null) {
            conversations = conversationRepository.findByUserIdAndProjectIdOrderByUpdatedAtDesc(userPrincipal.getId(), projectId);
        } else {
            conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userPrincipal.getId());
        }

        return conversations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(UserPrincipal userPrincipal, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        if (!conversation.getUserId().equals(userPrincipal.getId())) {
            throw new ForbiddenException("You do not have permission to access this conversation.");
        }

        var messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(msg -> new ConversationMessageResponse(
                        msg.getId(),
                        conversation.getId(),
                        msg.getRole(),
                        msg.getContent(),
                        msg.getFileId(),
                        msg.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getProjectId(),
                conversation.getTitle(),
                conversation.getProvider(),
                conversation.getModel(),
                messages,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public ConversationResponse createConversation(UserPrincipal userPrincipal, UUID projectId, String title, String provider, String model) {
        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));
            if (project.getOwnerId() != null && !project.getOwnerId().equals(userPrincipal.getId())) {
                throw new ForbiddenException("Access denied: You do not own this project.");
            }
        }

        String safeTitle = (title != null && !title.isBlank()) ? title.trim() : "New Chat";
        String safeProvider = (provider != null && !provider.isBlank()) ? provider.trim().toUpperCase() : "OPENAI";
        String safeModel = (model != null && !model.isBlank()) ? model.trim() : "gpt-4o-mini";

        Conversation conversation = new Conversation(null, userPrincipal.getId(), projectId, safeTitle, safeProvider, safeModel);
        Conversation saved = conversationRepository.save(conversation);
        return toResponse(saved);
    }

    public void deleteConversation(UserPrincipal userPrincipal, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        if (!conversation.getUserId().equals(userPrincipal.getId())) {
            throw new ForbiddenException("Access denied: You do not own this conversation.");
        }

        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    private ConversationResponse toResponse(Conversation c) {
        List<?> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
        return new ConversationResponse(
                c.getId(),
                c.getProjectId(),
                c.getTitle(),
                c.getProvider(),
                c.getModel(),
                msgs != null ? msgs.size() : 0,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
