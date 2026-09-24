package com.devpilot.ai.patch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChangeSetProposal {
    private UUID conversationId;
    private String summary;
    private List<FileChangeProposal> changes = new ArrayList<>();

    public ChangeSetProposal() {}

    public ChangeSetProposal(UUID conversationId, String summary, List<FileChangeProposal> changes) {
        this.conversationId = conversationId;
        this.summary = summary;
        this.changes = changes != null ? changes : new ArrayList<>();
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<FileChangeProposal> getChanges() {
        return changes;
    }

    public void setChanges(List<FileChangeProposal> changes) {
        this.changes = changes != null ? changes : new ArrayList<>();
    }
}
