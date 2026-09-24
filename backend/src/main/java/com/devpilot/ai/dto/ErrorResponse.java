package com.devpilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;
    private String message;
    private String path;
    private String timestamp = Instant.now().toString();
    private List<ValidationError> errors;

    public ErrorResponse() {}

    public ErrorResponse(boolean success, String message, String path, String timestamp, List<ValidationError> errors) {
        this.success = success;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp != null ? timestamp : Instant.now().toString();
        this.errors = errors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { this.errors = errors; }

    public static class ValidationError {
        private String field;
        private String reason;

        public ValidationError() {}

        public ValidationError(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class Builder {
        private boolean success = false;
        private String message;
        private String path;
        private String timestamp = Instant.now().toString();
        private List<ValidationError> errors;

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
        public Builder errors(List<ValidationError> errors) { this.errors = errors; return this; }

        public ErrorResponse build() {
            return new ErrorResponse(success, message, path, timestamp, errors);
        }
    }
}
