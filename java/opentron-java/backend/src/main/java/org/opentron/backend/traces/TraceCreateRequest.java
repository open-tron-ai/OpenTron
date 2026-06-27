package org.opentron.backend.traces;

import java.util.Map;

public class TraceCreateRequest {
    private String kind;
    private Map<String, Object> payload;

    public TraceCreateRequest() {}

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
}
