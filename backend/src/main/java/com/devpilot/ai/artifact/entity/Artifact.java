package com.devpilot.ai.artifact.entity;

import com.devpilot.ai.artifact.entity.enums.ArtifactType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artifacts")
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "build_id", nullable = false)
    private UUID buildId;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 32)
    private ArtifactType artifactType;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes = 0L;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Artifact() {}

    public Artifact(UUID id, UUID projectId, UUID buildId, ArtifactType artifactType, String storageKey, Long sizeBytes, String checksum, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.buildId = buildId;
        this.artifactType = artifactType;
        this.storageKey = storageKey;
        this.sizeBytes = sizeBytes != null ? sizeBytes : 0L;
        this.checksum = checksum;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getBuildId() { return buildId; }
    public void setBuildId(UUID buildId) { this.buildId = buildId; }

    public ArtifactType getArtifactType() { return artifactType; }
    public void setArtifactType(ArtifactType artifactType) { this.artifactType = artifactType; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static ArtifactBuilder builder() {
        return new ArtifactBuilder();
    }

    public static class ArtifactBuilder {
        private UUID id;
        private UUID projectId;
        private UUID buildId;
        private ArtifactType artifactType;
        private String storageKey;
        private Long sizeBytes = 0L;
        private String checksum;
        private Instant createdAt = Instant.now();

        public ArtifactBuilder id(UUID id) { this.id = id; return this; }
        public ArtifactBuilder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public ArtifactBuilder buildId(UUID buildId) { this.buildId = buildId; return this; }
        public ArtifactBuilder artifactType(ArtifactType artifactType) { this.artifactType = artifactType; return this; }
        public ArtifactBuilder storageKey(String storageKey) { this.storageKey = storageKey; return this; }
        public ArtifactBuilder sizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; return this; }
        public ArtifactBuilder checksum(String checksum) { this.checksum = checksum; return this; }
        public ArtifactBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Artifact build() {
            return new Artifact(id, projectId, buildId, artifactType, storageKey, sizeBytes, checksum, createdAt);
        }
    }
}
