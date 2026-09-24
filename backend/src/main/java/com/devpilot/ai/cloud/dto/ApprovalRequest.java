package com.devpilot.ai.cloud.dto;

public record ApprovalRequest(
        String approvedBy,
        String notes
) {}
