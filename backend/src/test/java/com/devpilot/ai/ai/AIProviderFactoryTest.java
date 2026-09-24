package com.devpilot.ai.ai;

import com.devpilot.ai.ai.provider.*;
import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.dto.ai.ProviderInfoResponse;
import com.devpilot.ai.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIProviderFactoryTest {

    private AIProperties properties;
    private OpenAIProvider openAIProvider;
    private GeminiProvider geminiProvider;
    private AnthropicProvider anthropicProvider;
    private SimulatedAIProvider simulatedProvider;
    private AIProviderFactory factory;

    @BeforeEach
    void setUp() {
        properties = new AIProperties();
        ObjectMapper mapper = new ObjectMapper();
        openAIProvider = new OpenAIProvider(properties, mapper);
        geminiProvider = new GeminiProvider(properties, mapper);
        anthropicProvider = new AnthropicProvider(properties, mapper);
        simulatedProvider = new SimulatedAIProvider();
        factory = new AIProviderFactory(openAIProvider, geminiProvider, anthropicProvider, simulatedProvider, properties);
    }

    @Test
    @DisplayName("Should resolve simulated provider when fallback is enabled and keys are empty")
    void testFallbackToSimulatedWhenUnconfigured() {
        properties.setFallbackEnabled(true);
        AIProvider provider = factory.getProvider("OPENAI");
        assertNotNull(provider);
        assertTrue(provider instanceof SimulatedAIProvider);
    }

    @Test
    @DisplayName("Should throw BadRequestException for unconfigured provider when fallback is disabled")
    void testThrowWhenFallbackDisabledAndNoKey() {
        properties.setFallbackEnabled(false);
        assertThrows(BadRequestException.class, () -> factory.getProvider("OPENAI"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for unknown provider")
    void testUnknownProvider() {
        assertThrows(BadRequestException.class, () -> factory.getProvider("NON_EXISTENT_AI"));
    }

    @Test
    @DisplayName("Should provide metadata about all supported providers")
    void testGetProvidersInfo() {
        ProviderInfoResponse info = factory.getProvidersInfo();
        assertNotNull(info);
        assertEquals(3, info.getProviders().size());
        assertTrue(info.getProviders().stream().anyMatch(p -> "OPENAI".equals(p.getId())));
        assertTrue(info.getProviders().stream().anyMatch(p -> "GEMINI".equals(p.getId())));
        assertTrue(info.getProviders().stream().anyMatch(p -> "ANTHROPIC".equals(p.getId())));
    }
}
