package com.devpilot.ai.cloud;

import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.provider.AwsCloudProvider;
import com.devpilot.ai.cloud.provider.CloudProvider;
import com.devpilot.ai.cloud.provider.LocalDockerCloudProvider;
import com.devpilot.ai.cloud.service.CloudProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CloudProviderFactoryTest {

    private CloudProviderFactory factory;

    @BeforeEach
    void setUp() {
        List<CloudProvider> providers = List.of(
                new LocalDockerCloudProvider(),
                new AwsCloudProvider()
        );
        factory = new CloudProviderFactory(providers);
    }

    @Test
    void testGetProviderSuccess() {
        CloudProvider localProvider = factory.getProvider(CloudProviderType.LOCAL_DOCKER);
        assertNotNull(localProvider);
        assertEquals(CloudProviderType.LOCAL_DOCKER, localProvider.getProviderType());

        CloudProvider awsProvider = factory.getProvider(CloudProviderType.AWS);
        assertNotNull(awsProvider);
        assertEquals(CloudProviderType.AWS, awsProvider.getProviderType());
    }

    @Test
    void testGetAvailableProviderTypes() {
        List<CloudProviderType> available = factory.getAvailableProviderTypes();
        assertTrue(available.contains(CloudProviderType.LOCAL_DOCKER));
        assertTrue(available.contains(CloudProviderType.AWS));
    }
}
