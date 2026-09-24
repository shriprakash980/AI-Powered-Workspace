package com.devpilot.ai.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateFolderRequest {

    @NotBlank(message = "Folder name cannot be empty")
    @Size(min = 1, max = 120, message = "Folder name must be between 1 and 120 characters")
    private String name;

    private UUID parentId;

    public CreateFolderRequest() {}

    public CreateFolderRequest(String name, UUID parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
}
