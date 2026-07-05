package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentTaskRequest {
    @NotBlank
    private String agent;
    @NotBlank
    private String task;

    public AgentTaskRequest() {}

    public AgentTaskRequest(String agent, String task) {
        this.agent = agent;
        this.task = task;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }
}
