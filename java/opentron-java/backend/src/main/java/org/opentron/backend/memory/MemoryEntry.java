package org.opentron.backend.memory;

import java.time.Instant;
import java.util.Map;

public class MemoryEntry {
    private String id;
    private String text;
    private Map<String, Object> metadata;
    private Instant createdAt;

    public MemoryEntry() {}

    public MemoryEntry(String id, String text, Map<String, Object> metadata) {
        this.id = id;
        this.text = text;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
