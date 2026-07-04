package org.opentron.backend.storage.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing agent execution traces and memories
 */
@Entity
@Table(name = "agent_memory")
public class AgentMemory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "agent_name", nullable = false, length = 255)
    private String agentName;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "raw_trace", columnDefinition = "TEXT")
    private String rawTrace;
    
    @Column(name = "compressed_summary", columnDefinition = "TEXT")
    private String compressedSummary;
    
    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] embedding;
    
    @Column(name = "relevance_score", columnDefinition = "DOUBLE PRECISION")
    private Double relevanceScore;
    
    @Column(name = "trace_hash", length = 64, unique = true)
    private String traceHash;
    
    @Column(name = "is_archived", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isArchived = false;
    
    // Constructors
    public AgentMemory() {}
    
    public AgentMemory(String agentName, String rawTrace, String compressedSummary) {
        this.agentName = agentName;
        this.rawTrace = rawTrace;
        this.compressedSummary = compressedSummary;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getRawTrace() { return rawTrace; }
    public void setRawTrace(String rawTrace) { this.rawTrace = rawTrace; }
    
    public String getCompressedSummary() { return compressedSummary; }
    public void setCompressedSummary(String compressedSummary) { this.compressedSummary = compressedSummary; }
    
    public byte[] getEmbedding() { return embedding; }
    public void setEmbedding(byte[] embedding) { this.embedding = embedding; }
    
    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
    
    public String getTraceHash() { return traceHash; }
    public void setTraceHash(String traceHash) { this.traceHash = traceHash; }
    
    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }
}
