package com.devpilot.ai.cloud.model;

public enum DeploymentEventType {
    QUEUED,
    BUILDING,
    PUSHING_IMAGE,
    DEPLOYING,
    HEALTH_CHECK,
    APPROVAL_REQUESTED,
    APPROVED,
    REJECTED,
    SUCCESS,
    FAILED,
    ROLLBACK,
    CANCELLED
}
