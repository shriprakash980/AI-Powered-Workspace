package com.devpilot.ai.build;

import com.devpilot.ai.build.dto.BuildRequest;
import com.devpilot.ai.build.dto.BuildResponse;
import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.Build;
import com.devpilot.ai.build.entity.enums.BuildMode;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.build.repository.BuildRepository;
import com.devpilot.ai.build.service.BuildDetectionService;
import com.devpilot.ai.build.service.BuildRunner;
import com.devpilot.ai.build.service.BuildService;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.git.service.GitWorkspaceManager;
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
class BuildServiceTest {

    @Mock
    private BuildRepository buildRepository;

    @Mock
    private BuildDetectionService buildDetectionService;

    @Mock
    private BuildRunner buildRunner;

    @Mock
    private ProjectService projectService;

    @Mock
    private GitWorkspaceManager gitWorkspaceManager;

    @InjectMocks
    private BuildService buildService;

    private UUID projectId;
    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("developer@devpilot.ai");

        testProject = new Project();
        testProject.setId(projectId);
        testProject.setName("demo-app");
        testProject.setOwnerId(testUser.getId());
    }

    @Test
    @DisplayName("Should successfully trigger build for valid project")
    void testTriggerBuildSuccess() {
        UUID buildId = UUID.randomUUID();
        when(projectService.getProjectEntityByIdAndUser(projectId, testUser.getId())).thenReturn(testProject);
        when(buildRepository.countByUserIdAndStatus(testUser.getId(), BuildStatus.BUILDING)).thenReturn(0L);

        ProjectDetectionResponse detection = new ProjectDetectionResponse(
                List.of(ProjectType.NODEJS), ProjectType.NODEJS, "npm run build", "npm test", "npm start", "dist", null
        );
        when(buildDetectionService.detectProjectType(projectId)).thenReturn(detection);

        Build mockBuild = new Build();
        mockBuild.setId(buildId);
        mockBuild.setProjectId(projectId);
        mockBuild.setUserId(testUser.getId());
        mockBuild.setProjectType(ProjectType.NODEJS);
        mockBuild.setMode(BuildMode.BUILD);
        mockBuild.setStatus(BuildStatus.QUEUED);

        when(buildRepository.save(any(Build.class))).thenReturn(mockBuild);

        BuildRequest request = new BuildRequest(BuildMode.BUILD, null);
        BuildResponse response = buildService.triggerBuild(projectId, testUser, request);

        assertNotNull(response);
        assertEquals(buildId, response.buildId());
        verify(buildRunner).executeBuildAsync(mockBuild);
    }

    @Test
    @DisplayName("Should throw exception when active build limit is exceeded")
    void testTriggerBuildLimitExceeded() {
        when(projectService.getProjectEntityByIdAndUser(projectId, testUser.getId())).thenReturn(testProject);
        when(buildRepository.countByUserIdAndStatus(testUser.getId(), BuildStatus.BUILDING)).thenReturn(5L);

        BuildRequest request = new BuildRequest(BuildMode.BUILD, null);

        assertThrows(IllegalStateException.class, () -> buildService.triggerBuild(projectId, testUser, request));
    }
}
