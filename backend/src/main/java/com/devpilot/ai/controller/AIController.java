package com.devpilot.ai.controller;

import com.devpilot.ai.ai.provider.AIProviderFactory;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.ai.*;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.repository.UserRepository;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Coding Assistant", description = "Endpoints for AI-powered multi-provider chat, streaming, and code actions")
@SecurityRequirement(name = "BearerAuth")
public class AIController {

    private final AIService aiService;
    private final AIProviderFactory providerFactory;
    private final UserRepository userRepository;

    public AIController(AIService aiService, AIProviderFactory providerFactory, UserRepository userRepository) {
        this.aiService = aiService;
        this.providerFactory = providerFactory;
        this.userRepository = userRepository;
    }

    @GetMapping("/providers")
    @Operation(summary = "Get available AI providers", description = "Lists supported AI providers (OpenAI, Gemini, Anthropic), configured status, and models.")
    public ResponseEntity<ApiResponse<ProviderInfoResponse>> getProviders() {
        ProviderInfoResponse info = providerFactory.getProvidersInfo();
        return ResponseEntity.ok(ApiResponse.success(info, "AI providers retrieved successfully"));
    }

    @PostMapping("/chat")
    @Operation(summary = "Synchronous AI Chat", description = "Sends a chat message to the AI assistant with workspace file context.")
    public ResponseEntity<ApiResponse<AIChatResponse>> chat(
            @Valid @RequestBody AIChatRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AIChatResponse response = aiService.chat(principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "AI response generated successfully"));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming AI Chat", description = "Streams AI responses token-by-token using Server-Sent Events (SSE).")
    public SseEmitter streamChat(
            @Valid @RequestBody AIChatRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout
        aiService.streamChat(principal, request, emitter);
        return emitter;
    }

    @PostMapping("/code/explain")
    @Operation(summary = "Explain Code", description = "Analyzes and explains the selected code snippet.")
    public ResponseEntity<ApiResponse<AICodeActionResponse>> explainCode(
            @Valid @RequestBody AICodeActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AICodeActionResponse response = aiService.executeCodeAction(principal, "EXPLAIN", request);
        return ResponseEntity.ok(ApiResponse.success(response, "Code explanation generated successfully"));
    }

    @PostMapping("/code/fix")
    @Operation(summary = "Fix Code Bug", description = "Diagnoses bugs in the selected code and proposes a replacement fix with diff preview.")
    public ResponseEntity<ApiResponse<AICodeActionResponse>> fixCode(
            @Valid @RequestBody AICodeActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AICodeActionResponse response = aiService.executeCodeAction(principal, "FIX", request);
        return ResponseEntity.ok(ApiResponse.success(response, "Code fix generated successfully"));
    }

    @PostMapping("/code/generate")
    @Operation(summary = "Generate Code", description = "Generates new code based on prompt instructions.")
    public ResponseEntity<ApiResponse<AICodeActionResponse>> generateCode(
            @Valid @RequestBody AICodeActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AICodeActionResponse response = aiService.executeCodeAction(principal, "GENERATE", request);
        return ResponseEntity.ok(ApiResponse.success(response, "Code generated successfully"));
    }

    @PostMapping("/code/refactor")
    @Operation(summary = "Refactor Code", description = "Optimizes code structure, readability, and performance with diff preview.")
    public ResponseEntity<ApiResponse<AICodeActionResponse>> refactorCode(
            @Valid @RequestBody AICodeActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AICodeActionResponse response = aiService.executeCodeAction(principal, "REFACTOR", request);
        return ResponseEntity.ok(ApiResponse.success(response, "Code refactored successfully"));
    }

    @PostMapping("/code/tests")
    @Operation(summary = "Generate Unit Tests", description = "Generates comprehensive automated unit tests for the selected code.")
    public ResponseEntity<ApiResponse<AICodeActionResponse>> generateTests(
            @Valid @RequestBody AICodeActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AICodeActionResponse response = aiService.executeCodeAction(principal, "TESTS", request);
        return ResponseEntity.ok(ApiResponse.success(response, "Unit tests generated successfully"));
    }

    @PostMapping("/code/debug")
    @Operation(summary = "Debug Code", description = "Assists in debugging errors, exceptions, or runtime defects.")
    public ResponseEntity<ApiResponse<AICodeActionResponse>> debugCode(
            @Valid @RequestBody AICodeActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AICodeActionResponse response = aiService.executeCodeAction(principal, "DEBUG", request);
        return ResponseEntity.ok(ApiResponse.success(response, "Debug analysis generated successfully"));
    }

    @PostMapping("/code/document")
    @Operation(summary = "Document Code", description = "Generates docstrings, Javadoc, and documentation for the selected code.")
    public ResponseEntity<ApiResponse<AICodeActionResponse>> documentCode(
            @Valid @RequestBody AICodeActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Authentication authentication) {
        UserPrincipal principal = resolvePrincipal(userPrincipal, authentication);
        AICodeActionResponse response = aiService.executeCodeAction(principal, "DOCUMENT", request);
        return ResponseEntity.ok(ApiResponse.success(response, "Code documentation generated successfully"));
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
