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
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private static final List<String> MODELS = List.of("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp");

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiProvider(AIProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public AIProviderType getProviderType() {
        return AIProviderType.GEMINI;
    }

    @Override
    public String getDefaultModel() {
        String configured = aiProperties.getGemini().getModel();
        return (configured != null && !configured.isBlank()) ? configured : "gemini-1.5-flash";
    }

    @Override
    public List<String> getAvailableModels() {
        return MODELS;
    }

    @Override
    public boolean isConfigured() {
        return aiProperties.getGemini().isConfigured();
    }

    @Override
    public AIResponse generate(AIRequest request) {
        if (!isConfigured()) {
            throw new BadRequestException("Google Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable or system property.");
        }

        long startTime = System.currentTimeMillis();
        String model = resolveModel(request.getModel());

        try {
            Map<String, Object> payload = buildPayload(request);
            String jsonBody = objectMapper.writeValueAsString(payload);

            String url = aiProperties.getGemini().getBaseUrl() + "/models/" + model + ":generateContent?key=" + aiProperties.getGemini().getApiKey();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 400) {
                log.error("Gemini API call failed with status {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("Gemini API error (" + response.statusCode() + "): " + extractErrorMessage(response.body()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractCandidateText(root);

            int promptTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
            int completionTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);
            int totalTokens = root.path("usageMetadata").path("totalTokenCount").asInt(promptTokens + completionTokens);

            return new AIResponse(content, getProviderType().name(), model, promptTokens, completionTokens, totalTokens, latency, "stop");

        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Failed to execute Gemini request", e);
            throw new RuntimeException("Failed to generate response from Google Gemini: " + e.getMessage(), e);
        }
    }

    @Override
    public void generateStream(AIRequest request, AIStreamConsumer consumer) {
        if (!isConfigured()) {
            consumer.onError(new BadRequestException("Google Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable."));
            return;
        }

        String model = resolveModel(request.getModel());

        try {
            Map<String, Object> payload = buildPayload(request);
            String jsonBody = objectMapper.writeValueAsString(payload);

            String url = aiProperties.getGemini().getBaseUrl() + "/models/" + model + ":streamGenerateContent?alt=sse&key=" + aiProperties.getGemini().getApiKey();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                consumer.onError(new BadRequestException("Gemini API error (" + response.statusCode() + "): " + extractErrorMessage(errorBody)));
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
                            String text = extractCandidateText(chunk);
                            if (!text.isEmpty()) {
                                consumer.onNext(text);
                            }
                        } catch (Exception ex) {
                            log.debug("Skipping unparseable Gemini SSE chunk: {}", data);
                        }
                    }
                }
            }
            consumer.onComplete();

        } catch (Exception e) {
            log.error("Streaming error in GeminiProvider", e);
            consumer.onError(e);
        }
    }

    private Map<String, Object> buildPayload(AIRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();

        String systemPrompt = null;
        List<Map<String, Object>> contents = new ArrayList<>();

        for (ChatMessage msg : request.getMessages()) {
            if ("system".equalsIgnoreCase(msg.getRole())) {
                systemPrompt = (systemPrompt == null) ? msg.getContent() : systemPrompt + "\n" + msg.getContent();
            } else {
                String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }

        if (contents.isEmpty()) {
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", "Hello"))));
        }

        payload.put("contents", contents);
        payload.put("generationConfig", Map.of(
                "temperature", request.getTemperature(),
                "maxOutputTokens", request.getMaxTokens()
        ));

        return payload;
    }

    private String extractCandidateText(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        sb.append(part.path("text").asText(""));
                    }
                }
                return sb.toString();
            }
        }
        return "";
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
