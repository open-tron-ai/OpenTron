package org.opentron.backend.dto;

import java.util.Map;

public class ManagedAgentCreateRequest {
    private String name;
    private String template_id;
    private Map<String, Object> config;

    public ManagedAgentCreateRequest() {}

    public ManagedAgentCreateRequest(String name, String template_id, Map<String, Object> config) {
        this.name = name;
        this.template_id = template_id;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
