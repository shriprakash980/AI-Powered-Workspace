package com.devpilot.ai.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateFileRequest {

    @NotBlank(message = "File name cannot be empty")
    @Size(min = 1, max = 120, message = "File name must be between 1 and 120 characters")
    private String name;

    private UUID parentId;

    @Size(max = 1000000, message = "File content exceeds maximum allowed size of 1MB")
    private String content;

    public CreateFileRequest() {}

    public CreateFileRequest(String name, UUID parentId, String content) {
        this.name = name;
        this.parentId = parentId;
        this.content = content;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
