package com.devpilot.ai.entity.enums;

public enum AIProviderType {
    OPENAI("OpenAI"),
    GEMINI("Google Gemini"),
    ANTHROPIC("Anthropic Claude");

    private final String displayName;

    AIProviderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AIProviderType fromString(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return OPENAI;
        }
        for (AIProviderType type : values()) {
            if (type.name().equalsIgnoreCase(provider.trim()) || type.displayName.equalsIgnoreCase(provider.trim())) {
                return type;
            }
        }
        return OPENAI;
    }
}
