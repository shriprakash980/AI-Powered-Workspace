package com.devpilot.ai.cloud.provider;

public interface ContainerRegistryProvider {
    boolean authenticate(String registryUrl, String username, String token);
    String pushImage(String localImageTag, String targetRegistryTag);
    boolean imageExists(String targetRegistryTag);
}
