package com.devpilot.ai.cloud.service;

import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.provider.CloudProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CloudProviderFactory {

    private final Map<CloudProviderType, CloudProvider> providerMap = new EnumMap<>(CloudProviderType.class);

    public CloudProviderFactory(List<CloudProvider> providers) {
        for (CloudProvider provider : providers) {
            providerMap.put(provider.getProviderType(), provider);
        }
    }

    public CloudProvider getProvider(CloudProviderType type) {
        CloudProvider provider = providerMap.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported or unconfigured cloud provider type: " + type);
        }
        return provider;
    }

    public Optional<CloudProvider> findProvider(CloudProviderType type) {
        return Optional.ofNullable(providerMap.get(type));
    }

    public List<CloudProviderType> getAvailableProviderTypes() {
        return providerMap.entrySet().stream()
                .filter(entry -> entry.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .toList();
    }
}
