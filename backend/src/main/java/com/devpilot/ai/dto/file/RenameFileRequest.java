package com.devpilot.ai.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RenameFileRequest {

    @NotBlank(message = "New name cannot be empty")
    @Size(min = 1, max = 120, message = "Name must be between 1 and 120 characters")
    private String name;

    public RenameFileRequest() {}

    public RenameFileRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
