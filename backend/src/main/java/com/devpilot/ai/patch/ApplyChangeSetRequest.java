package com.devpilot.ai.patch;

import java.util.List;
import java.util.UUID;

public class ApplyChangeSetRequest {
    private List<UUID> selectedChangeIds;

    public ApplyChangeSetRequest() {}

    public ApplyChangeSetRequest(List<UUID> selectedChangeIds) {
        this.selectedChangeIds = selectedChangeIds;
    }

    public List<UUID> getSelectedChangeIds() {
        return selectedChangeIds;
    }

    public void setSelectedChangeIds(List<UUID> selectedChangeIds) {
        this.selectedChangeIds = selectedChangeIds;
    }

    public List<UUID> getFileChangeIds() {
        return selectedChangeIds;
    }

    public void setFileChangeIds(List<UUID> fileChangeIds) {
        this.selectedChangeIds = fileChangeIds;
    }
}
