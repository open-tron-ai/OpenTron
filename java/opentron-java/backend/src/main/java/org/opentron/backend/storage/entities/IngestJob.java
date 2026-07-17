package org.opentron.backend.storage.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ingest_jobs")
public class IngestJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String jobId;

    @Column(name = "connector_type")
    private String connectorType;

    @Column(name = "status")
    private String status;

    @Column(name = "message", length = 2048)
    private String message;

    @Column(name = "items_processed")
    private Integer itemsProcessed = 0;

    @Column(name = "items_failed")
    private Integer itemsFailed = 0;

    @Column(name = "bytes_ingested")
    private Long bytesIngested = 0L;

    @Column(name = "chunks_created")
    private Integer chunksCreated = 0;

    @Column(name = "source")
    private String source;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public IngestJob() {}

    public IngestJob(String jobId, String connectorType) {
        this.jobId = jobId;
        this.connectorType = connectorType;
        this.status = "queued";
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getItemsProcessed() { return itemsProcessed; }
    public void setItemsProcessed(Integer itemsProcessed) { this.itemsProcessed = itemsProcessed; }

    public Integer getItemsFailed() { return itemsFailed; }
    public void setItemsFailed(Integer itemsFailed) { this.itemsFailed = itemsFailed; }

    public Long getBytesIngested() { return bytesIngested; }
    public void setBytesIngested(Long bytesIngested) { this.bytesIngested = bytesIngested; }

    public Integer getChunksCreated() { return chunksCreated; }
    public void setChunksCreated(Integer chunksCreated) { this.chunksCreated = chunksCreated; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public long getDurationMs() {
        if (startedAt == null || completedAt == null) return 0;
        return completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }
}
