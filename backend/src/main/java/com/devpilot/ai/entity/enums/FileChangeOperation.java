package com.devpilot.ai.entity.enums;

public enum FileChangeOperation {
    CREATE,
    UPDATE,
    DELETE,
    RENAME;

    public static FileChangeOperation fromString(String op) {
        if (op == null || op.isBlank()) return UPDATE;
        try {
            return FileChangeOperation.valueOf(op.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UPDATE;
        }
    }
}
