package com.devpilot.ai.service;

import com.devpilot.ai.ai.model.*;
import com.devpilot.ai.ai.prompt.AIContextBuilder;
import com.devpilot.ai.ai.prompt.PromptTemplates;
import com.devpilot.ai.ai.provider.AIProvider;
import com.devpilot.ai.ai.provider.AIProviderFactory;
import com.devpilot.ai.ai.util.DiffGenerator;
import com.devpilot.ai.dto.ai.AIChatRequest;
import com.devpilot.ai.dto.ai.AIChatResponse;
import com.devpilot.ai.dto.ai.AICodeActionRequest;
import com.devpilot.ai.dto.ai.AICodeActionResponse;
import com.devpilot.ai.entity.*;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.*;
import com.devpilot.ai.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final AIProviderFactory providerFactory;
    private final AIContextBuilder contextBuilder;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final AIRequestLogRepository requestLogRepository;
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository fileRepository;
    private final ObjectMapper objectMapper;
    private final com.devpilot.ai.context.ContextService contextService;

    public AIService(AIProviderFactory providerFactory,
                     AIContextBuilder contextBuilder,
                     ConversationRepository conversationRepository,
                     ConversationMessageRepository messageRepository,
                     AIRequestLogRepository requestLogRepository,
                     ProjectRepository projectRepository,
                     ProjectFileRepository fileRepository,
                     ObjectMapper objectMapper,
                     com.devpilot.ai.context.ContextService contextService) {
        this.providerFactory = providerFactory;
        this.contextBuilder = contextBuilder;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.projectRepository = projectRepository;
        this.fileRepository = fileRepository;
        this.objectMapper = objectMapper;
        this.contextService = contextService;
    }

    public AIChatResponse chat(UserPrincipal userPrincipal, AIChatRequest request) {
        Conversation conversation = resolveOrCreateConversation(userPrincipal, request);
        Project project = (conversation.getProjectId() != null) ? projectRepository.findById(conversation.getProjectId()).orElse(null) : null;
        ProjectFile activeFile = resolveFile(request.getFileId(), project);

        // Save user message
        ConversationMessage userMessage = new ConversationMessage(
                null,
                conversation.getId(),
                "user",
                request.getMessage(),
                activeFile != null ? activeFile.getId() : null
        );
        messageRepository.save(userMessage);

        String projectName = project != null ? project.getName() : null;
        String filePath = activeFile != null ? activeFile.getPath() : null;
        String fileContent = activeFile != null ? activeFile.getContent() : null;

        AIContext ctx = contextBuilder.build(
                project != null ? project.getId() : null,
                projectName,
                activeFile != null ? activeFile.getId() : null,
                filePath,
                fileContent,
                request.getSelectedCode(),
                request.getLanguage(),
                null,
                request.getMessage()
        );

        if (project != null) {
            try {
                String projectCtx = contextService.buildContextForPrompt(
                        project.getId(),
                        activeFile != null ? activeFile.getId() : null,
                        request.getSelectedCode(),
                        null,
                        null,
                        request.getMessage(),
                        userPrincipal
                );
                ctx.setProjectContext(projectCtx);
            } catch (Exception e) {
                log.warn("Could not enrich project context for chat: {}", e.getMessage());
            }
        }

        String systemPrompt = PromptTemplates.buildChatSystemPrompt(ctx);

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("system", systemPrompt));

        List<ConversationMessage> recentMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        int historyStart = Math.max(0, recentMessages.size() - 10);
        for (int i = historyStart; i < recentMessages.size(); i++) {
            ConversationMessage m = recentMessages.get(i);
            chatMessages.add(new ChatMessage(m.getRole(), m.getContent()));
        }

        AIProvider provider = providerFactory.getProvider(conversation.getProvider());
        String modelToUse = (request.getModel() != null && !request.getModel().isBlank()) ? request.getModel() : conversation.getModel();

        AIRequest aiRequest = new AIRequest(modelToUse, chatMessages, 0.2, 4096, false);

        long start = System.currentTimeMillis();
        AIResponse response;
        try {
            response = provider.generate(aiRequest);
        } catch (Exception e) {
            logRequest(userPrincipal.getId(), conversation.getProjectId(), conversation.getId(), provider.getProviderType().name(), modelToUse, "CHAT", 0, 0, System.currentTimeMillis() - start, "FAILED", e.getMessage());
            throw e;
        }

        ConversationMessage assistantMessage = new ConversationMessage(
                null,
                conversation.getId(),
                "assistant",
                response.getContent(),
                activeFile != null ? activeFile.getId() : null
        );
        ConversationMessage savedAssistantMsg = messageRepository.save(assistantMessage);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        logRequest(userPrincipal.getId(), conversation.getProjectId(), conversation.getId(), response.getProvider(), response.getModel(), "CHAT", response.getPromptTokens(), response.getCompletionTokens(), response.getLatencyMs(), "SUCCESS", null);

        return new AIChatResponse(
                conversation.getId(),
                savedAssistantMsg.getId(),
                "assistant",
                response.getContent(),
                response.getProvider(),
                response.getModel(),
                response.getPromptTokens(),
                response.getCompletionTokens(),
                response.getLatencyMs(),
                savedAssistantMsg.getCreatedAt()
        );
    }

    public void streamChat(UserPrincipal userPrincipal, AIChatRequest request, SseEmitter emitter) {
        Conversation conversation;
        try {
            conversation = resolveOrCreateConversation(userPrincipal, request);
        } catch (Exception e) {
            emitter.completeWithError(e);
            return;
        }

        Project project = (conversation.getProjectId() != null) ? projectRepository.findById(conversation.getProjectId()).orElse(null) : null;
        ProjectFile activeFile = resolveFile(request.getFileId(), project);

        ConversationMessage userMessage = new ConversationMessage(
                null,
                conversation.getId(),
                "user",
                request.getMessage(),
                activeFile != null ? activeFile.getId() : null
        );
        messageRepository.save(userMessage);

        String projectName = project != null ? project.getName() : null;
        String filePath = activeFile != null ? activeFile.getPath() : null;
        String fileContent = activeFile != null ? activeFile.getContent() : null;

        AIContext ctx = contextBuilder.build(
                project != null ? project.getId() : null,
                projectName,
                activeFile != null ? activeFile.getId() : null,
                filePath,
                fileContent,
                request.getSelectedCode(),
                request.getLanguage(),
                null,
                request.getMessage()
        );

        if (project != null) {
            try {
                String projectCtx = contextService.buildContextForPrompt(
                        project.getId(),
                        activeFile != null ? activeFile.getId() : null,
                        request.getSelectedCode(),
                        null,
                        null,
                        request.getMessage(),
                        userPrincipal
                );
                ctx.setProjectContext(projectCtx);
            } catch (Exception e) {
                log.warn("Could not enrich project context for chat stream: {}", e.getMessage());
            }
        }

        String systemPrompt = PromptTemplates.buildChatSystemPrompt(ctx);
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("system", systemPrompt));

        List<ConversationMessage> recentMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        int historyStart = Math.max(0, recentMessages.size() - 10);
        for (int i = historyStart; i < recentMessages.size(); i++) {
            ConversationMessage m = recentMessages.get(i);
            chatMessages.add(new ChatMessage(m.getRole(), m.getContent()));
        }

        AIProvider provider = providerFactory.getProvider(conversation.getProvider());
        String modelToUse = (request.getModel() != null && !request.getModel().isBlank()) ? request.getModel() : conversation.getModel();
        AIRequest aiRequest = new AIRequest(modelToUse, chatMessages, 0.2, 4096, true);

        long start = System.currentTimeMillis();
        StringBuilder responseBuffer = new StringBuilder();

        Thread.startVirtualThread(() -> {
            try {
                Map<String, Object> meta = Map.of(
                        "type", "meta",
                        "conversationId", conversation.getId().toString(),
                        "provider", provider.getProviderType().name(),
                        "model", modelToUse
                );
                emitter.send(SseEmitter.event().name("meta").data(objectMapper.writeValueAsString(meta)));

                provider.generateStream(aiRequest, new AIStreamConsumer() {
                    @Override
                    public void onNext(String token) {
                        try {
                            responseBuffer.append(token);
                            Map<String, String> data = Map.of("token", token);
                            emitter.send(SseEmitter.event().name("chunk").data(objectMapper.writeValueAsString(data)));
                        } catch (IOException e) {
                            log.warn("Error sending SSE chunk: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {
                        try {
                            long latency = System.currentTimeMillis() - start;
                            String fullContent = responseBuffer.toString();

                            ConversationMessage assistantMsg = new ConversationMessage(
                                    null,
                                    conversation.getId(),
                                    "assistant",
                                    fullContent,
                                    activeFile != null ? activeFile.getId() : null
                            );
                            messageRepository.save(assistantMsg);
                            conversation.setUpdatedAt(Instant.now());
                            conversationRepository.save(conversation);

                            logRequest(userPrincipal.getId(), conversation.getProjectId(), conversation.getId(), provider.getProviderType().name(), modelToUse, "CHAT_STREAM", 0, fullContent.length() / 4, latency, "SUCCESS", null);

                            Map<String, Object> doneData = Map.of(
                                    "type", "done",
                                    "conversationId", conversation.getId().toString(),
                                    "messageId", assistantMsg.getId().toString(),
                                    "latencyMs", latency
                            );
                            emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(doneData)));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("Error finalizing stream", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        long latency = System.currentTimeMillis() - start;
                        logRequest(userPrincipal.getId(), conversation.getProjectId(), conversation.getId(), provider.getProviderType().name(), modelToUse, "CHAT_STREAM", 0, 0, latency, "FAILED", throwable.getMessage());
                        try {
                            Map<String, String> err = Map.of("error", throwable.getMessage() != null ? throwable.getMessage() : "Unknown stream error");
                            emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(err)));
                        } catch (Exception ignored) {}
                        emitter.completeWithError(throwable);
                    }
                });

            } catch (Exception e) {
                log.error("Fatal exception during AI stream dispatch", e);
                emitter.completeWithError(e);
            }
        });
    }

    public AICodeActionResponse executeCodeAction(UserPrincipal userPrincipal, String action, AICodeActionRequest request) {
        if (request.getSelectedCode() == null || request.getSelectedCode().isBlank()) {
            throw new BadRequestException("Selected code cannot be empty for code actions.");
        }

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.getProjectId()));
            if (project.getOwnerId() != null && !project.getOwnerId().equals(userPrincipal.getId())) {
                throw new ForbiddenException("Access denied: You do not own this project.");
            }
        }

        ProjectFile file = null;
        if (request.getFileId() != null) {
            file = fileRepository.findById(request.getFileId()).orElse(null);
        }

        String filePath = (file != null) ? file.getPath() : request.getFilePath();
        String fileContent = (file != null && file.getContent() != null) ? file.getContent() : request.getFileContent();

        AIContext ctx = contextBuilder.build(
                project != null ? project.getId() : null,
                project != null ? project.getName() : null,
                file != null ? file.getId() : null,
                filePath,
                fileContent,
                request.getSelectedCode(),
                request.getLanguage(),
                request.getInstruction(),
                null
        );

        if (project != null) {
            try {
                String projectCtx = contextService.buildContextForPrompt(
                        project.getId(),
                        file != null ? file.getId() : null,
                        request.getSelectedCode(),
                        null,
                        null,
                        request.getInstruction(),
                        userPrincipal
                );
                ctx.setProjectContext(projectCtx);
            } catch (Exception e) {
                log.warn("Could not enrich project context for code action: {}", e.getMessage());
            }
        }

        String prompt = PromptTemplates.buildCodeActionPrompt(action, ctx);
        String providerName = (request.getProvider() != null && !request.getProvider().isBlank()) ? request.getProvider() : null;
        AIProvider provider = (providerName != null) ? providerFactory.getProvider(providerName) : providerFactory.getDefaultProvider();

        String model = (request.getModel() != null && !request.getModel().isBlank()) ? request.getModel() : provider.getDefaultModel();

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", PromptTemplates.SYSTEM_PROMPT),
                new ChatMessage("user", prompt)
        );

        long start = System.currentTimeMillis();
        AIResponse response;
        try {
            response = provider.generate(new AIRequest(model, messages, 0.2, 4096, false));
        } catch (Exception e) {
            logRequest(userPrincipal.getId(), project != null ? project.getId() : null, null, provider.getProviderType().name(), model, action.toUpperCase(), 0, 0, System.currentTimeMillis() - start, "FAILED", e.getMessage());
            throw e;
        }

        String rawContent = response.getContent();
        String suggestedCode = DiffGenerator.extractCodeFromMarkdown(rawContent);
        String diff = "";

        if (!"EXPLAIN".equalsIgnoreCase(action) && suggestedCode != null && !suggestedCode.isBlank() && !suggestedCode.equals(request.getSelectedCode())) {
            diff = DiffGenerator.generateUnifiedDiff(request.getSelectedCode(), suggestedCode, filePath);
        }

        logRequest(userPrincipal.getId(), project != null ? project.getId() : null, null, response.getProvider(), response.getModel(), action.toUpperCase(), response.getPromptTokens(), response.getCompletionTokens(), response.getLatencyMs(), "SUCCESS", null);

        return new AICodeActionResponse(
                action.toUpperCase(),
                rawContent,
                request.getSelectedCode(),
                suggestedCode,
                diff,
                response.getProvider(),
                response.getModel(),
                response.getLatencyMs()
        );
    }

    private Conversation resolveOrCreateConversation(UserPrincipal userPrincipal, AIChatRequest request) {
        if (request.getConversationId() != null) {
            Conversation conv = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + request.getConversationId()));
            if (!conv.getUserId().equals(userPrincipal.getId())) {
                throw new ForbiddenException("Access denied: You do not own this conversation.");
            }
            if (request.getProvider() != null && !request.getProvider().isBlank()) {
                conv.setProvider(request.getProvider().trim().toUpperCase());
            }
            if (request.getModel() != null && !request.getModel().isBlank()) {
                conv.setModel(request.getModel().trim());
            }
            return conv;
        }

        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));
            if (project.getOwnerId() != null && !project.getOwnerId().equals(userPrincipal.getId())) {
                throw new ForbiddenException("Access denied: You do not own this project.");
            }
        }

        String rawMsg = request.getMessage().trim();
        String title = rawMsg.length() > 40 ? rawMsg.substring(0, 37) + "..." : rawMsg;
        String provider = (request.getProvider() != null && !request.getProvider().isBlank()) ? request.getProvider().trim().toUpperCase() : "OPENAI";
        String model = (request.getModel() != null && !request.getModel().isBlank()) ? request.getModel().trim() : "gpt-4o-mini";

        Conversation conversation = new Conversation(null, userPrincipal.getId(), request.getProjectId(), title, provider, model);
        return conversationRepository.save(conversation);
    }

    private ProjectFile resolveFile(UUID fileId, Project project) {
        if (fileId == null) return null;
        ProjectFile file = fileRepository.findById(fileId).orElse(null);
        if (file != null && project != null && !file.getProjectId().equals(project.getId())) {
            throw new BadRequestException("File does not belong to the active project.");
        }
        return file;
    }

    private void logRequest(UUID userId, UUID projectId, UUID conversationId, String provider, String model, String type, int promptTokens, int completionTokens, long latencyMs, String status, String error) {
        try {
            AIRequestLog logEntity = new AIRequestLog();
            logEntity.setUserId(userId);
            logEntity.setProjectId(projectId);
            logEntity.setConversationId(conversationId);
            logEntity.setProvider(provider != null ? provider : "UNKNOWN");
            logEntity.setModel(model != null ? model : "UNKNOWN");
            logEntity.setRequestType(type);
            logEntity.setPromptTokens(promptTokens);
            logEntity.setCompletionTokens(completionTokens);
            logEntity.setTotalTokens(promptTokens + completionTokens);
            logEntity.setLatencyMs(latencyMs);
            logEntity.setStatus(status);
            logEntity.setErrorMessage(error);
            requestLogRepository.save(logEntity);
        } catch (Exception ex) {
            log.warn("Failed to persist AI audit log: {}", ex.getMessage());
        }
    }
}
