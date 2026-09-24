package com.devpilot.ai.dto.ai;

import java.util.List;

public class ProviderInfoResponse {

    private String defaultProvider;
    private List<ProviderModelInfo> providers;

    public ProviderInfoResponse() {}

    public ProviderInfoResponse(String defaultProvider, List<ProviderModelInfo> providers) {
        this.defaultProvider = defaultProvider;
        this.providers = providers;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public List<ProviderModelInfo> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderModelInfo> providers) {
        this.providers = providers;
    }

    public static class ProviderModelInfo {
        private String id;
        private String name;
        private boolean configured;
        private String defaultModel;
        private List<String> availableModels;

        public ProviderModelInfo() {}

        public ProviderModelInfo(String id, String name, boolean configured, String defaultModel, List<String> availableModels) {
            this.id = id;
            this.name = name;
            this.configured = configured;
            this.defaultModel = defaultModel;
            this.availableModels = availableModels;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isConfigured() {
            return configured;
        }

        public void setConfigured(boolean configured) {
            this.configured = configured;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public List<String> getAvailableModels() {
            return availableModels;
        }

        public void setAvailableModels(List<String> availableModels) {
            this.availableModels = availableModels;
        }
    }
}
