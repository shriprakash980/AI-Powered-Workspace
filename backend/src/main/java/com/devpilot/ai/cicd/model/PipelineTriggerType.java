package com.devpilot.ai.cicd.model;

public enum PipelineTriggerType {
    PUSH,
    PULL_REQUEST,
    MANUAL,
    WEBHOOK,
    SCHEDULED
}
