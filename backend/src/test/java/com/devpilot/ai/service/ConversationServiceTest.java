package com.devpilot.ai.service;

import com.devpilot.ai.dto.ai.ConversationDetailResponse;
import com.devpilot.ai.dto.ai.ConversationResponse;
import com.devpilot.ai.entity.Conversation;
import com.devpilot.ai.entity.ConversationMessage;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ConversationMessageRepository;
import com.devpilot.ai.repository.ConversationRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ConversationService conversationService;

    private UserPrincipal userPrincipal;
    private UUID userId;
    private UUID conversationId;
    private Conversation sampleConversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        userPrincipal = new UserPrincipal(
                userId,
                "Test User",
                "test@devpilot.ai",
                "hashedPass",
                UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        sampleConversation = new Conversation(
                conversationId,
                userId,
                null,
                "Test Chat Session",
                "OPENAI",
                "gpt-4o-mini"
        );
        sampleConversation.setCreatedAt(Instant.now());
        sampleConversation.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should list conversations for authenticated user")
    void testListConversations() {
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId))
                .thenReturn(List.of(sampleConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(Collections.emptyList());

        List<ConversationResponse> list = conversationService.listConversations(userPrincipal, null);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Test Chat Session", list.get(0).getTitle());
    }

    @Test
    @DisplayName("Should get conversation detail with messages for owner")
    void testGetConversationSuccess() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(sampleConversation));
        ConversationMessage msg = new ConversationMessage(UUID.randomUUID(), conversationId, "user", "Hello AI", null);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(List.of(msg));

        ConversationDetailResponse detail = conversationService.getConversation(userPrincipal, conversationId);
        assertNotNull(detail);
        assertEquals(conversationId, detail.getId());
        assertEquals(1, detail.getMessages().size());
        assertEquals("Hello AI", detail.getMessages().get(0).getContent());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user accesses conversation owned by another user")
    void testGetConversationForbidden() {
        UUID otherUserId = UUID.randomUUID();
        Conversation otherConv = new Conversation(conversationId, otherUserId, null, "Other Chat", "OPENAI", "gpt-4o");
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(otherConv));

        assertThrows(ForbiddenException.class, () -> conversationService.getConversation(userPrincipal, conversationId));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when conversation ID does not exist")
    void testGetConversationNotFound() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> conversationService.getConversation(userPrincipal, conversationId));
    }

    @Test
    @DisplayName("Should create conversation successfully")
    void testCreateConversation() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        ConversationResponse created = conversationService.createConversation(userPrincipal, null, "New Chat Session", "OPENAI", "gpt-4o-mini");
        assertNotNull(created);
        assertEquals("New Chat Session", created.getTitle());
        assertEquals("OPENAI", created.getProvider());
    }

    @Test
    @DisplayName("Should delete conversation when invoked by owner")
    void testDeleteConversation() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(sampleConversation));

        assertDoesNotThrow(() -> conversationService.deleteConversation(userPrincipal, conversationId));
        verify(messageRepository, times(1)).deleteByConversationId(conversationId);
        verify(conversationRepository, times(1)).delete(sampleConversation);
    }
}
