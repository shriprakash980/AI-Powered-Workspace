package com.devpilot.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AIProperties {

    private String defaultProvider = "OPENAI";
    private int timeoutSeconds = 30;
    private boolean fallbackEnabled = true;

    private ProviderConfig openai = new ProviderConfig("gpt-4o-mini", "https://api.openai.com/v1");
    private ProviderConfig gemini = new ProviderConfig("gemini-1.5-flash", "https://generativelanguage.googleapis.com/v1beta");
    private ProviderConfig anthropic = new ProviderConfig("claude-3-5-sonnet-20241022", "https://api.anthropic.com/v1");

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public ProviderConfig getOpenai() {
        return openai;
    }

    public void setOpenai(ProviderConfig openai) {
        this.openai = openai;
    }

    public ProviderConfig getGemini() {
        return gemini;
    }

    public void setGemini(ProviderConfig gemini) {
        this.gemini = gemini;
    }

    public ProviderConfig getAnthropic() {
        return anthropic;
    }

    public void setAnthropic(ProviderConfig anthropic) {
        this.anthropic = anthropic;
    }

    public static class ProviderConfig {
        private String apiKey = "";
        private String model;
        private String baseUrl;

        public ProviderConfig() {}

        public ProviderConfig(String model, String baseUrl) {
            this.model = model;
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey != null ? apiKey.trim() : "";
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
