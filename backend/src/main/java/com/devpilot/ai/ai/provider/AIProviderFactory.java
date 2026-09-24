package com.devpilot.ai.ai.provider;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.dto.ai.ProviderInfoResponse;
import com.devpilot.ai.dto.ai.ProviderInfoResponse.ProviderModelInfo;
import com.devpilot.ai.entity.enums.AIProviderType;
import com.devpilot.ai.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AIProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AIProviderFactory.class);

    private final OpenAIProvider openAIProvider;
    private final GeminiProvider geminiProvider;
    private final AnthropicProvider anthropicProvider;
    private final SimulatedAIProvider simulatedAIProvider;
    private final AIProperties aiProperties;

    public AIProviderFactory(OpenAIProvider openAIProvider,
                             GeminiProvider geminiProvider,
                             AnthropicProvider anthropicProvider,
                             SimulatedAIProvider simulatedAIProvider,
                             AIProperties aiProperties) {
        this.openAIProvider = openAIProvider;
        this.geminiProvider = geminiProvider;
        this.anthropicProvider = anthropicProvider;
        this.simulatedAIProvider = simulatedAIProvider;
        this.aiProperties = aiProperties;
    }

    public AIProvider getProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return getDefaultProvider();
        }

        String normalized = providerName.trim().toUpperCase();
        return switch (normalized) {
            case "OPENAI" -> resolveOrFallback(openAIProvider, "OpenAI");
            case "GEMINI", "GOOGLE" -> resolveOrFallback(geminiProvider, "Google Gemini");
            case "ANTHROPIC", "CLAUDE" -> resolveOrFallback(anthropicProvider, "Anthropic Claude");
            case "SIMULATED", "MOCK" -> simulatedAIProvider;
            default -> throw new BadRequestException("Unsupported AI provider: " + providerName + ". Supported providers are: OPENAI, GEMINI, ANTHROPIC.");
        };
    }

    public AIProvider getDefaultProvider() {
        String configuredDefault = aiProperties.getDefaultProvider();
        try {
            return getProvider(configuredDefault);
        } catch (Exception e) {
            log.warn("Default provider '{}' resolution failed, falling back to simulated provider", configuredDefault);
            return simulatedAIProvider;
        }
    }

    private AIProvider resolveOrFallback(AIProvider provider, String displayName) {
        if (provider.isConfigured()) {
            return provider;
        }

        if (aiProperties.isFallbackEnabled()) {
            log.info("Provider {} has no API key configured. Utilizing local simulated AI engine.", displayName);
            return simulatedAIProvider;
        }

        throw new BadRequestException(displayName + " API key is not configured. Please supply API credentials in application settings or environment variables.");
    }

    public ProviderInfoResponse getProvidersInfo() {
        List<ProviderModelInfo> list = new ArrayList<>();
        list.add(new ProviderModelInfo(
                "OPENAI",
                "OpenAI",
                openAIProvider.isConfigured(),
                openAIProvider.getDefaultModel(),
                openAIProvider.getAvailableModels()
        ));
        list.add(new ProviderModelInfo(
                "GEMINI",
                "Google Gemini",
                geminiProvider.isConfigured(),
                geminiProvider.getDefaultModel(),
                geminiProvider.getAvailableModels()
        ));
        list.add(new ProviderModelInfo(
                "ANTHROPIC",
                "Anthropic Claude",
                anthropicProvider.isConfigured(),
                anthropicProvider.getDefaultModel(),
                anthropicProvider.getAvailableModels()
        ));

        return new ProviderInfoResponse(aiProperties.getDefaultProvider(), list);
    }
}
