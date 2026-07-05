package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentCoordinateRequest {
    @NotBlank
    private String request;
    private String context;

    public AgentCoordinateRequest() {}

    public AgentCoordinateRequest(String request, String context) {
        this.request = request;
        this.context = context;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
