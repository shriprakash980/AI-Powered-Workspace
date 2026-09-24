package com.devpilot.ai.artifact.service;

import com.devpilot.ai.artifact.dto.ArtifactDto;
import com.devpilot.ai.artifact.entity.Artifact;
import com.devpilot.ai.artifact.entity.enums.ArtifactType;
import com.devpilot.ai.artifact.repository.ArtifactRepository;
import com.devpilot.ai.artifact.storage.ArtifactStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final ArtifactStorage artifactStorage;

    public ArtifactService(ArtifactRepository artifactRepository, ArtifactStorage artifactStorage) {
        this.artifactRepository = artifactRepository;
        this.artifactStorage = artifactStorage;
    }

    @Transactional
    public ArtifactDto createArtifact(UUID projectId, UUID buildId, ArtifactType artifactType, String filename, byte[] content) {
        String storageKey = projectId + "/" + buildId + "/" + filename;
        String checksum = calculateChecksum(content);

        artifactStorage.store(new ByteArrayInputStream(content), storageKey);

        Artifact artifact = Artifact.builder()
                .projectId(projectId)
                .buildId(buildId)
                .artifactType(artifactType)
                .storageKey(storageKey)
                .sizeBytes((long) content.length)
                .checksum(checksum)
                .build();

        Artifact saved = artifactRepository.save(artifact);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ArtifactDto> getProjectArtifacts(UUID projectId) {
        return artifactRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtifactDto getArtifact(UUID artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));
        return mapToDto(artifact);
    }

    private String calculateChecksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private ArtifactDto mapToDto(Artifact a) {
        return new ArtifactDto(
                a.getId(),
                a.getProjectId(),
                a.getBuildId(),
                a.getArtifactType(),
                a.getStorageKey(),
                a.getSizeBytes(),
                a.getChecksum(),
                a.getCreatedAt()
        );
    }
}
