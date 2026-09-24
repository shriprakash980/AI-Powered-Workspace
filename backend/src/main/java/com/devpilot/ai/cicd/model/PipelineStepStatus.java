package com.devpilot.ai.cicd.model;

public enum PipelineStepStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    SKIPPED
}
