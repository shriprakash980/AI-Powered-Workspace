package com.devpilot.ai.ai.provider;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.AIStreamConsumer;
import com.devpilot.ai.entity.enums.AIProviderType;

import java.util.List;

public interface AIProvider {

    AIProviderType getProviderType();

    String getDefaultModel();

    List<String> getAvailableModels();

    boolean isConfigured();

    AIResponse generate(AIRequest request);

    void generateStream(AIRequest request, AIStreamConsumer consumer);
}
