package com.devpilot.ai.dto.ai;

public class AICodeActionResponse {

    private String action;
    private String explanation;
    private String originalCode;
    private String suggestedCode;
    private String diff;
    private String provider;
    private String model;
    private Long latencyMs;

    public AICodeActionResponse() {}

    public AICodeActionResponse(String action, String explanation, String originalCode, String suggestedCode, String diff, String provider, String model, Long latencyMs) {
        this.action = action;
        this.explanation = explanation;
        this.originalCode = originalCode;
        this.suggestedCode = suggestedCode;
        this.diff = diff;
        this.provider = provider;
        this.model = model;
        this.latencyMs = latencyMs;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getOriginalCode() {
        return originalCode;
    }

    public void setOriginalCode(String originalCode) {
        this.originalCode = originalCode;
    }

    public String getSuggestedCode() {
        return suggestedCode;
    }

    public void setSuggestedCode(String suggestedCode) {
        this.suggestedCode = suggestedCode;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
