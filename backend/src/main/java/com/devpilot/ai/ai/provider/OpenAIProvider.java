package com.devpilot.ai.ai.provider;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.AIStreamConsumer;
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
public class OpenAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);
    private static final List<String> MODELS = List.of("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo");

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAIProvider(AIProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public AIProviderType getProviderType() {
        return AIProviderType.OPENAI;
    }

    @Override
    public String getDefaultModel() {
        String configured = aiProperties.getOpenai().getModel();
        return (configured != null && !configured.isBlank()) ? configured : "gpt-4o-mini";
    }

    @Override
    public List<String> getAvailableModels() {
        return MODELS;
    }

    @Override
    public boolean isConfigured() {
        return aiProperties.getOpenai().isConfigured();
    }

    @Override
    public AIResponse generate(AIRequest request) {
        if (!isConfigured()) {
            throw new BadRequestException("OpenAI API key is not configured. Please set the OPENAI_API_KEY environment variable or system property.");
        }

        long startTime = System.currentTimeMillis();
        String model = resolveModel(request.getModel());

        try {
            Map<String, Object> payload = buildPayload(request, model, false);
            String jsonBody = objectMapper.writeValueAsString(payload);

            String url = aiProperties.getOpenai().getBaseUrl() + "/chat/completions";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getOpenai().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 400) {
                log.error("OpenAI API call failed with status {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("OpenAI API error (" + response.statusCode() + "): " + extractErrorMessage(response.body()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = "";
            String finishReason = "stop";
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode first = choices.get(0);
                content = first.path("message").path("content").asText("");
                finishReason = first.path("finish_reason").asText("stop");
            }

            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            int totalTokens = root.path("usage").path("total_tokens").asInt(0);

            return new AIResponse(content, getProviderType().name(), model, promptTokens, completionTokens, totalTokens, latency, finishReason);

        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Failed to execute OpenAI request", e);
            throw new RuntimeException("Failed to generate response from OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public void generateStream(AIRequest request, AIStreamConsumer consumer) {
        if (!isConfigured()) {
            consumer.onError(new BadRequestException("OpenAI API key is not configured. Please set the OPENAI_API_KEY environment variable."));
            return;
        }

        String model = resolveModel(request.getModel());

        try {
            Map<String, Object> payload = buildPayload(request, model, true);
            String jsonBody = objectMapper.writeValueAsString(payload);

            String url = aiProperties.getOpenai().getBaseUrl() + "/chat/completions";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getOpenai().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                consumer.onError(new BadRequestException("OpenAI API error (" + response.statusCode() + "): " + extractErrorMessage(errorBody)));
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith(":")) continue;
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            JsonNode chunk = objectMapper.readTree(data);
                            JsonNode choices = chunk.path("choices");
                            if (choices.isArray() && !choices.isEmpty()) {
                                JsonNode delta = choices.get(0).path("delta");
                                if (delta.has("content")) {
                                    String text = delta.path("content").asText("");
                                    if (!text.isEmpty()) {
                                        consumer.onNext(text);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            log.debug("Skipping unparseable SSE chunk: {}", data);
                        }
                    }
                }
            }
            consumer.onComplete();

        } catch (Exception e) {
            log.error("Streaming error in OpenAIProvider", e);
            consumer.onError(e);
        }
    }

    private Map<String, Object> buildPayload(AIRequest request, String model, boolean stream) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", request.getTemperature());
        payload.put("max_tokens", request.getMaxTokens());
        payload.put("stream", stream);

        List<Map<String, String>> messages = new ArrayList<>();
        for (var msg : request.getMessages()) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
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
