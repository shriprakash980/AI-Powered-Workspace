package com.devpilot.ai.deployment;

import com.devpilot.ai.deployment.dto.EnvironmentVariableRequest;
import com.devpilot.ai.deployment.dto.EnvironmentVariableResponse;
import com.devpilot.ai.deployment.entity.ProjectEnvironmentVariable;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.repository.ProjectEnvironmentVariableRepository;
import com.devpilot.ai.deployment.service.EnvironmentVariableService;
import com.devpilot.ai.git.security.TokenEncryptionService;
import com.devpilot.ai.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentVariableServiceTest {

    @Mock
    private ProjectEnvironmentVariableRepository envVarRepository;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private EnvironmentVariableService environmentVariableService;

    private UUID projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should encrypt value and save environment variable")
    void testSaveVariableSuccess() {
        EnvironmentVariableRequest req = new EnvironmentVariableRequest("API_PORT", "8080", DeploymentEnvironment.DEVELOPMENT);

        when(tokenEncryptionService.encrypt("8080")).thenReturn("encrypted_8080");

        ProjectEnvironmentVariable savedVar = ProjectEnvironmentVariable.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name("API_PORT")
                .encryptedValue("encrypted_8080")
                .environment(DeploymentEnvironment.DEVELOPMENT)
                .build();

        when(envVarRepository.findByProjectIdAndNameAndEnvironment(projectId, "API_PORT", DeploymentEnvironment.DEVELOPMENT))
                .thenReturn(Optional.empty());
        when(envVarRepository.save(any(ProjectEnvironmentVariable.class))).thenReturn(savedVar);

        EnvironmentVariableResponse resp = environmentVariableService.saveVariable(projectId, userId, req);

        assertNotNull(resp);
        assertEquals("API_PORT", resp.name());
        assertEquals("••••••••", resp.maskedValue());
        verify(tokenEncryptionService).encrypt("8080");
    }

    @Test
    @DisplayName("Should block overriding system secret keys like JWT_SECRET")
    void testBlockedSystemKey() {
        EnvironmentVariableRequest req = new EnvironmentVariableRequest("JWT_SECRET", "hacked", DeploymentEnvironment.DEVELOPMENT);

        assertThrows(IllegalArgumentException.class, () -> environmentVariableService.saveVariable(projectId, userId, req));
    }
}
