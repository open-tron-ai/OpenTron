package org.opentron.backend.storage.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing trace logs from agent execution
 */
@Entity
@Table(name = "trace_logs")
public class TraceLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String agent;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(columnDefinition = "TEXT")
    private String input;
    
    @Column(columnDefinition = "TEXT")
    private String output;
    
    @Column(name = "tools_used", columnDefinition = "TEXT")
    private String toolsUsed;
    
    @Column(name = "duration_ms")
    private Integer durationMs;
    
    @Column(name = "is_compressed", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isCompressed = false;
    
    @Column(name = "compressed_data", columnDefinition = "bytea DEFAULT NULL", nullable = true)
    private byte[] compressedData = null;
    
    // Constructors
    public TraceLog() {
        this.timestamp = LocalDateTime.now();
        this.compressedData = null;
    }
    
    public TraceLog(String agent, String input, String output, Integer durationMs) {
        this.agent = agent;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.timestamp = LocalDateTime.now();
        this.compressedData = null;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }
    
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    
    public String getToolsUsed() { return toolsUsed; }
    public void setToolsUsed(String toolsUsed) { this.toolsUsed = toolsUsed; }
    
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Boolean getIsCompressed() { return isCompressed; }
    public void setIsCompressed(Boolean isCompressed) { this.isCompressed = isCompressed; }
    
    public byte[] getCompressedData() { return compressedData; }
    public void setCompressedData(byte[] compressedData) { 
        this.compressedData = compressedData;
        if (compressedData != null) {
            this.isCompressed = true;
        }
    }
}
