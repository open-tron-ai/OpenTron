package org.opentron.backend.memory;

import java.util.Map;

public class MemoryStoreRequest {
    private String text;
    private Map<String, Object> metadata;

    public MemoryStoreRequest() {}

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
}
