package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.ai.ConversationDetailResponse;
import com.devpilot.ai.dto.ai.ConversationResponse;
import com.devpilot.ai.dto.ai.CreateConversationRequest;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.repository.UserRepository;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/conversations")
@Tag(name = "AI Conversations", description = "Endpoints for managing persistent AI chat conversation threads")
@SecurityRequirement(name = "BearerAuth")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService, UserRepository userRepository) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "List conversations", description = "Retrieves all conversation sessions for the current user, optionally filtered by project.")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations(
            @RequestParam(required = false) UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        List<ConversationResponse> list = conversationService.listConversations(principal, projectId);
        return ResponseEntity.ok(ApiResponse.success(list, "Conversations retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation details", description = "Retrieves full message history for a specific conversation.")
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        ConversationDetailResponse detail = conversationService.getConversation(principal, id);
        return ResponseEntity.ok(ApiResponse.success(detail, "Conversation details retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create conversation", description = "Starts a new conversation thread.")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @RequestBody(required = false) CreateConversationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        UUID projectId = request != null ? request.getProjectId() : null;
        String title = request != null ? request.getTitle() : "New Chat";
        String provider = request != null ? request.getProvider() : null;
        String model = request != null ? request.getModel() : null;

        ConversationResponse created = conversationService.createConversation(principal, projectId, title, provider, model);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Conversation created successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete conversation", description = "Deletes a conversation and its messages.")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        conversationService.deleteConversation(principal, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation deleted successfully"));
    }

    private UserPrincipal resolvePrincipal(UserPrincipal principal, Authentication authentication) {
        if (principal != null) return principal;
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName())
                    .map(UserPrincipal::create)
                    .orElseGet(() -> new UserPrincipal(
                            UUID.fromString("00000000-0000-0000-0000-000000000001"),
                            "DevPilot User",
                            authentication.getName(),
                            "",
                            UserStatus.ACTIVE,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    ));
        }
        return new UserPrincipal(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "DevPilot User",
                "developer@devpilot.ai",
                "",
                UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
