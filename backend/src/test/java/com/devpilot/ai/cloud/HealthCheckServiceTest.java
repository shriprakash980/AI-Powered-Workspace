package com.devpilot.ai.cloud;

import com.devpilot.ai.cloud.entity.DeploymentTarget;
import com.devpilot.ai.cloud.entity.ServiceHealthCheck;
import com.devpilot.ai.cloud.model.CloudProviderType;
import com.devpilot.ai.cloud.model.HealthStatus;
import com.devpilot.ai.cloud.provider.CloudProvider;
import com.devpilot.ai.cloud.repository.DeploymentTargetRepository;
import com.devpilot.ai.cloud.repository.ServiceHealthCheckRepository;
import com.devpilot.ai.cloud.service.CloudProviderFactory;
import com.devpilot.ai.cloud.service.HealthCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HealthCheckServiceTest {

    private DeploymentTargetRepository deploymentTargetRepository;
    private ServiceHealthCheckRepository healthCheckRepository;
    private CloudProviderFactory providerFactory;
    private CloudProvider mockProvider;

    private HealthCheckService healthCheckService;

    @BeforeEach
    void setUp() {
        deploymentTargetRepository = mock(DeploymentTargetRepository.class);
        healthCheckRepository = mock(ServiceHealthCheckRepository.class);
        providerFactory = mock(CloudProviderFactory.class);
        mockProvider = mock(CloudProvider.class);

        healthCheckService = new HealthCheckService(deploymentTargetRepository, healthCheckRepository, providerFactory);
    }

    @Test
    void testPerformHealthCheckSuccess() {
        UUID projectId = UUID.randomUUID();
        DeploymentTarget target = new DeploymentTarget();
        target.setId(UUID.randomUUID());
        target.setProjectId(projectId);
        target.setProvider(CloudProviderType.LOCAL_DOCKER);
        target.setCustomDomain("http://localhost:8080");

        when(providerFactory.getProvider(CloudProviderType.LOCAL_DOCKER)).thenReturn(mockProvider);
        when(mockProvider.checkHealth(target)).thenReturn(HealthStatus.HEALTHY);
        when(healthCheckRepository.save(any(ServiceHealthCheck.class))).thenAnswer(i -> i.getArgument(0));

        ServiceHealthCheck check = healthCheckService.performHealthCheck(target);

        assertNotNull(check);
        assertEquals(HealthStatus.HEALTHY, check.getStatus());
        verify(deploymentTargetRepository, times(1)).save(target);
    }
}
