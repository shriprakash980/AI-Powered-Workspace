package com.devpilot.ai.service;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.prompt.AIContextBuilder;
import com.devpilot.ai.ai.provider.AIProvider;
import com.devpilot.ai.ai.provider.AIProviderFactory;
import com.devpilot.ai.dto.ai.AIChatRequest;
import com.devpilot.ai.dto.ai.AIChatResponse;
import com.devpilot.ai.dto.ai.AICodeActionRequest;
import com.devpilot.ai.dto.ai.AICodeActionResponse;
import com.devpilot.ai.entity.*;
import com.devpilot.ai.entity.enums.AIProviderType;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devpilot.ai.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @Mock
    private AIRequestLogRepository requestLogRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectFileRepository fileRepository;

    @Mock
    private AIProvider mockProvider;

    private AIService aiService;
    private UserPrincipal userPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userPrincipal = new UserPrincipal(
                userId,
                "Developer",
                "dev@devpilot.ai",
                "secret",
                UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        AIContextBuilder contextBuilder = new AIContextBuilder();
        ObjectMapper objectMapper = new ObjectMapper();

        aiService = new AIService(
                providerFactory,
                contextBuilder,
                conversationRepository,
                messageRepository,
                requestLogRepository,
                projectRepository,
                fileRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should successfully execute chat and persist user and assistant messages")
    void testChatSuccess() {
        AIChatRequest request = new AIChatRequest(
                null, null, null, "How do I optimize a binary search tree in Java?",
                "OPENAI", "gpt-4o-mini", null, "java", false
        );

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(inv -> {
            ConversationMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });

        when(mockProvider.generate(any(AIRequest.class))).thenReturn(new AIResponse(
                "You can optimize a BST by keeping it balanced using an AVL or Red-Black structure.",
                "OPENAI", "gpt-4o-mini", 12, 18, 30, 200L, "stop"
        ));
        when(providerFactory.getProvider(anyString())).thenReturn(mockProvider);

        AIChatResponse response = aiService.chat(userPrincipal, request);

        assertNotNull(response);
        assertEquals("assistant", response.getRole());
        assertTrue(response.getContent().contains("AVL or Red-Black"));
        verify(messageRepository, times(2)).save(any(ConversationMessage.class));
        verify(requestLogRepository, times(1)).save(any(AIRequestLog.class));
    }

    @Test
    @DisplayName("Should reject code action if selected code is empty")
    void testCodeActionEmptySelection() {
        AICodeActionRequest request = new AICodeActionRequest();
        request.setSelectedCode("   ");

        assertThrows(BadRequestException.class, () -> aiService.executeCodeAction(userPrincipal, "EXPLAIN", request));
    }

    @Test
    @DisplayName("Should reject code action if user does not own the project")
    void testCodeActionForbiddenProject() {
        UUID projectId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        project.setOwnerId(otherUserId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        AICodeActionRequest request = new AICodeActionRequest();
        request.setProjectId(projectId);
        request.setSelectedCode("int x = 10;");

        assertThrows(ForbiddenException.class, () -> aiService.executeCodeAction(userPrincipal, "FIX", request));
    }

    @Test
    @DisplayName("Should execute FIX code action and compute diff preview")
    void testCodeActionFixWithDiff() {
        AICodeActionRequest request = new AICodeActionRequest();
        request.setSelectedCode("int res = 10 / 0;");
        request.setInstruction("Avoid divide by zero");
        request.setLanguage("java");

        when(mockProvider.getProviderType()).thenReturn(AIProviderType.OPENAI);
        when(mockProvider.getDefaultModel()).thenReturn("gpt-4o-mini");
        when(mockProvider.generate(any(AIRequest.class))).thenReturn(new AIResponse(
                "Here is the fixed code:\n```java\nint res = divisor != 0 ? 10 / divisor : 0;\n```",
                "OPENAI", "gpt-4o-mini", 20, 25, 45, 150L, "stop"
        ));
        when(providerFactory.getDefaultProvider()).thenReturn(mockProvider);

        AICodeActionResponse response = aiService.executeCodeAction(userPrincipal, "FIX", request);

        assertNotNull(response);
        assertEquals("FIX", response.getAction());
        assertEquals("int res = divisor != 0 ? 10 / divisor : 0;", response.getSuggestedCode());
        assertFalse(response.getDiff().isBlank());
        assertTrue(response.getDiff().contains("-int res = 10 / 0;"));
        assertTrue(response.getDiff().contains("+int res = divisor != 0 ? 10 / divisor : 0;"));
    }
}
