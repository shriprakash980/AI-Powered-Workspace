package com.devpilot.ai.entity;

import com.devpilot.ai.entity.enums.FileChangeOperation;
import com.devpilot.ai.entity.enums.FileChangeStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_file_changes")
public class FileChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changeset_id", nullable = false)
    private ChangeSet changeSet;

    @Column(name = "file_id")
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 30)
    private FileChangeOperation operation = FileChangeOperation.UPDATE;

    @Column(name = "old_path", length = 500)
    private String oldPath;

    @Column(name = "new_path", length = 500)
    private String newPath;

    @Column(name = "old_content_hash", length = 64)
    private String oldContentHash;

    @Column(name = "proposed_content_hash", length = 64)
    private String proposedContentHash;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "proposed_content", columnDefinition = "TEXT")
    private String proposedContent;

    @Column(name = "diff", columnDefinition = "TEXT")
    private String diff;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FileChangeStatus status = FileChangeStatus.PROPOSED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    public FileChange() {}

    public FileChange(UUID id, ChangeSet changeSet, UUID fileId, FileChangeOperation operation, String oldPath, String newPath, String oldContentHash, String proposedContentHash, String originalContent, String proposedContent, String diff, FileChangeStatus status) {
        this.id = id;
        this.changeSet = changeSet;
        this.fileId = fileId;
        this.operation = operation;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.oldContentHash = oldContentHash;
        this.proposedContentHash = proposedContentHash;
        this.originalContent = originalContent;
        this.proposedContent = proposedContent;
        this.diff = diff;
        this.status = status != null ? status : FileChangeStatus.PROPOSED;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ChangeSet getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public FileChangeOperation getOperation() {
        return operation;
    }

    public void setOperation(FileChangeOperation operation) {
        this.operation = operation;
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getOldContentHash() {
        return oldContentHash;
    }

    public void setOldContentHash(String oldContentHash) {
        this.oldContentHash = oldContentHash;
    }

    public String getProposedContentHash() {
        return proposedContentHash;
    }

    public void setProposedContentHash(String proposedContentHash) {
        this.proposedContentHash = proposedContentHash;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getProposedContent() {
        return proposedContent;
    }

    public void setProposedContent(String proposedContent) {
        this.proposedContent = proposedContent;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public FileChangeStatus getStatus() {
        return status;
    }

    public void setStatus(FileChangeStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }
}
