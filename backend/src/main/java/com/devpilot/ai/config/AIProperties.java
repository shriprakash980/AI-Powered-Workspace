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

    private ContextConfig context = new ContextConfig();
    private PatchConfig patch = new PatchConfig();
    private VersionConfig version = new VersionConfig();

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

    public ContextConfig getContext() {
        return context;
    }

    public void setContext(ContextConfig context) {
        this.context = context;
    }

    public PatchConfig getPatch() {
        return patch;
    }

    public void setPatch(PatchConfig patch) {
        this.patch = patch;
    }

    public VersionConfig getVersion() {
        return version;
    }

    public void setVersion(VersionConfig version) {
        this.version = version;
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

    public static class ContextConfig {
        private int maxFiles = 20;
        private int maxChars = 50000;
        private int maxChunkChars = 12000;
        private int maxTotalChars = 50000;

        public int getMaxFiles() {
            return maxFiles;
        }

        public void setMaxFiles(int maxFiles) {
            this.maxFiles = maxFiles;
        }

        public int getMaxChars() {
            return maxChars;
        }

        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }

        public int getMaxChunkChars() {
            return maxChunkChars;
        }

        public void setMaxChunkChars(int maxChunkChars) {
            this.maxChunkChars = maxChunkChars;
        }

        public int getMaxTotalChars() {
            return maxTotalChars;
        }

        public void setMaxTotalChars(int maxTotalChars) {
            this.maxTotalChars = maxTotalChars;
        }
    }

    public static class PatchConfig {
        private int maxFiles = 10;
        private int maxFileSize = 500000;
        private int maxTotalSize = 2000000;
        private int maxEditsPerFile = 100;

        public int getMaxFiles() {
            return maxFiles;
        }

        public void setMaxFiles(int maxFiles) {
            this.maxFiles = maxFiles;
        }

        public int getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(int maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public int getMaxTotalSize() {
            return maxTotalSize;
        }

        public void setMaxTotalSize(int maxTotalSize) {
            this.maxTotalSize = maxTotalSize;
        }

        public int getMaxEditsPerFile() {
            return maxEditsPerFile;
        }

        public void setMaxEditsPerFile(int maxEditsPerFile) {
            this.maxEditsPerFile = maxEditsPerFile;
        }
    }

    public static class VersionConfig {
        private int maxContentSize = 500000;

        public int getMaxContentSize() {
            return maxContentSize;
        }

        public void setMaxContentSize(int maxContentSize) {
            this.maxContentSize = maxContentSize;
        }
    }
}
