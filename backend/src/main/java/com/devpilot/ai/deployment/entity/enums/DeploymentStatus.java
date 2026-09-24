package com.devpilot.ai.deployment.entity.enums;

public enum DeploymentStatus {
    QUEUED,
    PREPARING,
    DEPLOYING,
    STARTING,
    RUNNING,
    FAILED,
    STOPPED,
    ROLLING_BACK,
    ROLLED_BACK
}
