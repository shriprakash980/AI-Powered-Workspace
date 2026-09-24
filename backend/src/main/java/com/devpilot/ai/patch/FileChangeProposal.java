package com.devpilot.ai.patch;

import com.devpilot.ai.entity.enums.FileChangeOperation;

import java.util.ArrayList;
import java.util.List;

public class FileChangeProposal {
    private FileChangeOperation operation = FileChangeOperation.UPDATE;
    private String filePath;
    private String oldPath;
    private String newPath;
    private String proposedContent;
    private List<TextEdit> edits = new ArrayList<>();
    private String reason;

    public FileChangeProposal() {}

    public FileChangeProposal(FileChangeOperation operation, String filePath, String proposedContent, String reason) {
        this.operation = operation;
        this.filePath = filePath;
        this.proposedContent = proposedContent;
        this.reason = reason;
    }

    public FileChangeOperation getOperation() {
        return operation;
    }

    public void setOperation(FileChangeOperation operation) {
        this.operation = operation;
    }

    public String getFilePath() {
        return filePath != null ? filePath : (oldPath != null ? oldPath : newPath);
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getOldPath() {
        return oldPath != null ? oldPath : filePath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath != null ? newPath : filePath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getProposedContent() {
        return proposedContent;
    }

    public void setProposedContent(String proposedContent) {
        this.proposedContent = proposedContent;
    }

    public List<TextEdit> getEdits() {
        return edits;
    }

    public void setEdits(List<TextEdit> edits) {
        this.edits = edits != null ? edits : new ArrayList<>();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
