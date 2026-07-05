package org.opentron.backend.dto;

import java.util.Map;

public class ManagedAgentUpdateRequest {
    private Map<String, Object> config;

    public ManagedAgentUpdateRequest() {}

    public ManagedAgentUpdateRequest(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
