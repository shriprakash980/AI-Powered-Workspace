package com.devpilot.ai.cicd.model;

public enum PipelineStepType {
    CHECKOUT,
    INSTALL,
    BUILD,
    TEST,
    PACKAGE,
    ARTIFACT,
    IMAGE_BUILD,
    DEPLOY,
    HEALTH_CHECK
}
