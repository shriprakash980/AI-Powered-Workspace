package com.devpilot.ai.entity.enums;

public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MessageRole fromString(String role) {
        if (role == null || role.trim().isEmpty()) {
            return USER;
        }
        for (MessageRole r : values()) {
            if (r.name().equalsIgnoreCase(role.trim()) || r.value.equalsIgnoreCase(role.trim())) {
                return r;
            }
        }
        return USER;
    }
}
