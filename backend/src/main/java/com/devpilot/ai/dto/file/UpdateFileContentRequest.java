package com.devpilot.ai.dto.file;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateFileContentRequest {

    @NotNull(message = "File content cannot be null")
    @Size(max = 1000000, message = "File content exceeds maximum allowed size of 1MB")
    private String content;

    public UpdateFileContentRequest() {}

    public UpdateFileContentRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
