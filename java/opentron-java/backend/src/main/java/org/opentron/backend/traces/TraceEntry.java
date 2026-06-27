package org.opentron.backend.traces;

import java.time.Instant;
import java.util.Map;

public class TraceEntry {
    private String id;
    private String kind;
    private Map<String, Object> payload;
    private Instant createdAt;

    public TraceEntry() {}

    public TraceEntry(String id, String kind, Map<String, Object> payload) {
        this.id = id;
        this.kind = kind;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
