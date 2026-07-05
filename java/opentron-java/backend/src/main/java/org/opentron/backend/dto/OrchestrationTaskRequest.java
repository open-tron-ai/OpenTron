package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class OrchestrationTaskRequest {
    @NotBlank
    private String task;
    private String context;

    public OrchestrationTaskRequest() {}

    public OrchestrationTaskRequest(String task, String context) {
        this.task = task;
        this.context = context;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
