package com.devpilot.ai.context;

public class ContextException extends RuntimeException {

    private final String errorCode;

    public ContextException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
