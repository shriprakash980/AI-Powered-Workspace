package com.devpilot.ai.deployment;

import com.devpilot.ai.artifact.service.ArtifactService;
import com.devpilot.ai.build.repository.BuildRepository;
import com.devpilot.ai.build.service.BuildService;
import com.devpilot.ai.deployment.dto.DeploymentRequest;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;
import com.devpilot.ai.deployment.provider.DeploymentProvider;
import com.devpilot.ai.deployment.repository.DeploymentRepository;
import com.devpilot.ai.deployment.service.DeploymentService;
import com.devpilot.ai.deployment.service.EnvironmentVariableService;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private DeploymentProvider deploymentProvider;

    @Mock
    private EnvironmentVariableService environmentVariableService;

    @Mock
    private ArtifactService artifactService;

    @Mock
    private BuildRepository buildRepository;

    @Mock
    private BuildService buildService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private DeploymentService deploymentService;

    private UUID projectId;
    private User testUser;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should throw exception when active deployment limit per user is exceeded")
    void testCreateDeploymentLimitExceeded() {
        when(deploymentRepository.countByUserIdAndStatus(testUser.getId(), DeploymentStatus.RUNNING)).thenReturn(2L);

        DeploymentRequest req = new DeploymentRequest(DeploymentEnvironment.DEVELOPMENT, null);

        assertThrows(IllegalStateException.class, () -> deploymentService.createDeployment(projectId, testUser, req));
    }
}
