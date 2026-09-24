package com.devpilot.ai.ai.provider;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.AIStreamConsumer;
import com.devpilot.ai.ai.model.ChatMessage;
import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.entity.enums.AIProviderType;
import com.devpilot.ai.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
public class AnthropicProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final List<String> MODELS = List.of("claude-3-5-sonnet-20241022", "claude-3-haiku-20240307", "claude-3-opus-20240229");

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AnthropicProvider(AIProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public AIProviderType getProviderType() {
        return AIProviderType.ANTHROPIC;
    }

    @Override
    public String getDefaultModel() {
        String configured = aiProperties.getAnthropic().getModel();
        return (configured != null && !configured.isBlank()) ? configured : "claude-3-5-sonnet-20241022";
    }

    @Override
    public List<String> getAvailableModels() {
        return MODELS;
    }

    @Override
    public boolean isConfigured() {
        return aiProperties.getAnthropic().isConfigured();
    }

    @Override
    public AIResponse generate(AIRequest request) {
        if (!isConfigured()) {
            throw new BadRequestException("Anthropic API key is not configured. Please set the ANTHROPIC_API_KEY environment variable or system property.");
        }

        long startTime = System.currentTimeMillis();
        String model = resolveModel(request.getModel());

        try {
            Map<String, Object> payload = buildPayload(request, model, false);
            String jsonBody = objectMapper.writeValueAsString(payload);

            String url = aiProperties.getAnthropic().getBaseUrl() + "/messages";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", aiProperties.getAnthropic().getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 400) {
                log.error("Anthropic API call failed with status {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("Anthropic API error (" + response.statusCode() + "): " + extractErrorMessage(response.body()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = "";
            JsonNode contentArr = root.path("content");
            if (contentArr.isArray() && !contentArr.isEmpty()) {
                content = contentArr.get(0).path("text").asText("");
            }

            int promptTokens = root.path("usage").path("input_tokens").asInt(0);
            int completionTokens = root.path("usage").path("output_tokens").asInt(0);
            int totalTokens = promptTokens + completionTokens;

            return new AIResponse(content, getProviderType().name(), model, promptTokens, completionTokens, totalTokens, latency, root.path("stop_reason").asText("end_turn"));

        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Failed to execute Anthropic request", e);
            throw new RuntimeException("Failed to generate response from Anthropic Claude: " + e.getMessage(), e);
        }
    }

    @Override
    public void generateStream(AIRequest request, AIStreamConsumer consumer) {
        if (!isConfigured()) {
            consumer.onError(new BadRequestException("Anthropic API key is not configured. Please set the ANTHROPIC_API_KEY environment variable."));
            return;
        }

        String model = resolveModel(request.getModel());

        try {
            Map<String, Object> payload = buildPayload(request, model, true);
            String jsonBody = objectMapper.writeValueAsString(payload);

            String url = aiProperties.getAnthropic().getBaseUrl() + "/messages";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", aiProperties.getAnthropic().getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                consumer.onError(new BadRequestException("Anthropic API error (" + response.statusCode() + "): " + extractErrorMessage(errorBody)));
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith(":")) continue;
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        try {
                            JsonNode chunk = objectMapper.readTree(data);
                            String type = chunk.path("type").asText("");
                            if ("content_block_delta".equals(type)) {
                                String text = chunk.path("delta").path("text").asText("");
                                if (!text.isEmpty()) {
                                    consumer.onNext(text);
                                }
                            } else if ("message_stop".equals(type)) {
                                break;
                            }
                        } catch (Exception ex) {
                            log.debug("Skipping unparseable Anthropic SSE chunk: {}", data);
                        }
                    }
                }
            }
            consumer.onComplete();

        } catch (Exception e) {
            log.error("Streaming error in AnthropicProvider", e);
            consumer.onError(e);
        }
    }

    private Map<String, Object> buildPayload(AIRequest request, String model, boolean stream) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("max_tokens", request.getMaxTokens());
        payload.put("temperature", request.getTemperature());
        payload.put("stream", stream);

        String systemPrompt = null;
        List<Map<String, String>> messages = new ArrayList<>();

        for (ChatMessage msg : request.getMessages()) {
            if ("system".equalsIgnoreCase(msg.getRole())) {
                systemPrompt = (systemPrompt == null) ? msg.getContent() : systemPrompt + "\n" + msg.getContent();
            } else {
                String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "assistant" : "user";
                messages.add(Map.of("role", role, "content", msg.getContent()));
            }
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            payload.put("system", systemPrompt);
        }

        if (messages.isEmpty()) {
            messages.add(Map.of("role", "user", "content", "Hello"));
        }

        payload.put("messages", messages);
        return payload;
    }

    private String resolveModel(String requestedModel) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel.trim();
        }
        return getDefaultModel();
    }

    private String extractErrorMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error") && root.path("error").has("message")) {
                return root.path("error").path("message").asText();
            }
        } catch (Exception ignored) {}
        return body;
    }
}
