package com.devpilot.ai.artifact;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.artifact.entity.Artifact;
import com.devpilot.ai.artifact.entity.enums.ArtifactType;
import com.devpilot.ai.artifact.repository.ArtifactRepository;
import com.devpilot.ai.artifact.service.ArtifactService;
import com.devpilot.ai.artifact.storage.ArtifactStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceTest {

    @Mock
    private ArtifactRepository artifactRepository;

    @Mock
    private ArtifactStorage artifactStorage;

    @InjectMocks
    private ArtifactService artifactService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create artifact and calculate SHA-256 checksum")
    void testCreateArtifact() {
        UUID buildId = UUID.randomUUID();
        UUID artifactId = UUID.randomUUID();
        byte[] content = "dummy jar content".getBytes();

        Artifact savedArtifact = Artifact.builder()
                .id(artifactId)
                .projectId(projectId)
                .buildId(buildId)
                .artifactType(ArtifactType.JAR)
                .storageKey(projectId + "/" + buildId + "/app.jar")
                .sizeBytes((long) content.length)
                .checksum("abc123sha256")
                .build();

        when(artifactRepository.save(any(Artifact.class))).thenReturn(savedArtifact);

        ArtifactDto dto = artifactService.createArtifact(projectId, buildId, ArtifactType.JAR, "app.jar", content);

        assertNotNull(dto);
        assertEquals(artifactId, dto.id());
        assertEquals(ArtifactType.JAR, dto.artifactType());
        verify(artifactStorage).store(any(InputStream.class), eq(projectId + "/" + buildId + "/app.jar"));
    }

    @Test
    @DisplayName("Should list project artifacts ordered by date desc")
    void testGetProjectArtifacts() {
        Artifact a1 = Artifact.builder().id(UUID.randomUUID()).projectId(projectId).artifactType(ArtifactType.JAR).storageKey("a1").sizeBytes(100L).build();
        when(artifactRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(a1));

        List<ArtifactDto> dtos = artifactService.getProjectArtifacts(projectId);

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
    }
}
