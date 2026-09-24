package com.devpilot.ai.git.exception;

import java.util.Collections;
import java.util.List;

public class GitConflictException extends RuntimeException {

    private final String conflictType;
    private final List<String> conflictingFiles;

    public GitConflictException(String message) {
        this(message, "MERGE_CONFLICT", Collections.emptyList());
    }

    public GitConflictException(String message, String conflictType, List<String> conflictingFiles) {
        super(message);
        this.conflictType = conflictType;
        this.conflictingFiles = conflictingFiles != null ? conflictingFiles : Collections.emptyList();
    }

    public String getConflictType() {
        return conflictType;
    }

    public List<String> getConflictingFiles() {
        return conflictingFiles;
    }
}
