package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class MemoryStoreRequest {
    private String agent_name;

    @NotBlank
    private String content;

    private String summary;

    public MemoryStoreRequest() {}

    public MemoryStoreRequest(String agent_name, String content, String summary) {
        this.agent_name = agent_name;
        this.content = content;
        this.summary = summary;
    }

    public String getAgent_name() {
        return agent_name;
    }

    public void setAgent_name(String agent_name) {
        this.agent_name = agent_name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
