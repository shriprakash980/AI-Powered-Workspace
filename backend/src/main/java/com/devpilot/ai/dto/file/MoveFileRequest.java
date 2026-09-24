package com.devpilot.ai.dto.file;

import java.util.UUID;

public class MoveFileRequest {

    private UUID parentId;

    public MoveFileRequest() {}

    public MoveFileRequest(UUID parentId) {
        this.parentId = parentId;
    }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
}
