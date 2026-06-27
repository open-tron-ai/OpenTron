package org.opentron.backend.workflow;

public class WorkflowRunRequest {
    private String workflow;
    private String input;

    public WorkflowRunRequest() {
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
