package com.devpilot.ai.ai.model;

import java.util.ArrayList;
import java.util.List;

public class AIRequest {
    private String model;
    private List<ChatMessage> messages = new ArrayList<>();
    private double temperature = 0.2;
    private int maxTokens = 4096;
    private boolean stream = false;

    public AIRequest() {}

    public AIRequest(String model, List<ChatMessage> messages, double temperature, int maxTokens, boolean stream) {
        this.model = model;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}
