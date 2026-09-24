package com.devpilot.ai.cicd;

import com.devpilot.ai.cicd.dto.PipelineCreateRequest;
import com.devpilot.ai.cicd.dto.PipelineResponse;
import com.devpilot.ai.cicd.entity.Pipeline;
import com.devpilot.ai.cicd.repository.PipelineRepository;
import com.devpilot.ai.cicd.service.PipelineRunService;
import com.devpilot.ai.cicd.service.PipelineService;
import com.devpilot.ai.cicd.service.PipelineTriggerService;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private PipelineTriggerService triggerService;

    @Mock
    private PipelineRunService runService;

    @InjectMocks
    private PipelineService pipelineService;

    private UUID projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should successfully create pipeline for project")
    void testCreatePipeline() {
        PipelineCreateRequest req = new PipelineCreateRequest(
                "Production CI", "Main branch build pipeline", true, "devpilot-ci.yml", "main", true, true, true, false, DeploymentEnvironment.DEVELOPMENT
        );

        Pipeline mockPipeline = new Pipeline(
                UUID.randomUUID(), projectId, req.name(), req.description(), true, "devpilot-ci.yml", "main", true, true, true, false, DeploymentEnvironment.DEVELOPMENT, null, null
        );

        when(pipelineRepository.save(any(Pipeline.class))).thenReturn(mockPipeline);

        PipelineResponse response = pipelineService.createPipeline(projectId, userId, req);

        assertNotNull(response);
        assertEquals("Production CI", response.name());
        verify(projectService).verifyProjectOwnership(projectId, userId);
    }

    @Test
    @DisplayName("Should return default pipeline if none exists for project")
    void testGetProjectPipelinesDefaultCreation() {
        when(pipelineRepository.findByProjectId(projectId)).thenReturn(List.of());

        Pipeline defaultMock = new Pipeline(
                UUID.randomUUID(), projectId, "Default CI/CD Pipeline", "Automated pipeline", true, "devpilot-ci.yml", "main", true, true, true, false, DeploymentEnvironment.DEVELOPMENT, null, null
        );
        when(pipelineRepository.save(any(Pipeline.class))).thenReturn(defaultMock);

        List<PipelineResponse> pipelines = pipelineService.getProjectPipelines(projectId, userId);

        assertNotNull(pipelines);
        assertEquals(1, pipelines.size());
        assertEquals("Default CI/CD Pipeline", pipelines.get(0).name());
    }
}
