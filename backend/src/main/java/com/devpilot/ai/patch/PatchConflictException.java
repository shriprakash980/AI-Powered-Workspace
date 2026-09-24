package com.devpilot.ai.patch;

public class PatchConflictException extends RuntimeException {

    private final PatchConflict conflict;

    public PatchConflictException(PatchConflict conflict) {
        super(conflict.getMessage());
        this.conflict = conflict;
    }

    public PatchConflict getConflict() {
        return conflict;
    }
}
