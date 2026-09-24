package com.devpilot.ai.patch;

import java.util.ArrayList;
import java.util.List;

public class PatchValidationResult {
    private boolean valid;
    private List<String> errors = new ArrayList<>();

    public PatchValidationResult() {
        this.valid = true;
    }

    public PatchValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public static PatchValidationResult ok() {
        return new PatchValidationResult(true, new ArrayList<>());
    }

    public static PatchValidationResult error(String error) {
        PatchValidationResult res = new PatchValidationResult(false, new ArrayList<>());
        res.addError(error);
        return res;
    }

    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
