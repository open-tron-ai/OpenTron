package org.opentron.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class OrchestrationBatchRequest {

    @NotNull
    @Valid
    private List<OrchestrationTaskRequest> tasks;

    public OrchestrationBatchRequest() {}

    public OrchestrationBatchRequest(List<OrchestrationTaskRequest> tasks) {
        this.tasks = tasks;
    }

    public List<OrchestrationTaskRequest> getTasks() {
        return tasks;
    }

    public void setTasks(List<OrchestrationTaskRequest> tasks) {
        this.tasks = tasks;
    }
}
