package com.devpilot.ai.build.entity;

import com.devpilot.ai.build.entity.enums.LogType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "build_logs")
public class BuildLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "build_id", nullable = false)
    private UUID buildId;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 16)
    private LogType logType = LogType.INFO;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BuildLog() {}

    public BuildLog(UUID id, UUID buildId, LogType logType, String message, Integer sequenceNumber, Instant createdAt) {
        this.id = id;
        this.buildId = buildId;
        this.logType = logType != null ? logType : LogType.INFO;
        this.message = message;
        this.sequenceNumber = sequenceNumber;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBuildId() { return buildId; }
    public void setBuildId(UUID buildId) { this.buildId = buildId; }

    public LogType getLogType() { return logType; }
    public void setLogType(LogType logType) { this.logType = logType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
