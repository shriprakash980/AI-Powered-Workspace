package com.devpilot.ai.cloud.dto;

import com.devpilot.ai.cloud.model.CloudProviderType;

import java.time.Instant;
import java.util.UUID;

public record CloudCredentialResponse(
        UUID id,
        String name,
        CloudProviderType provider,
        String region,
        Instant createdAt
) {}
