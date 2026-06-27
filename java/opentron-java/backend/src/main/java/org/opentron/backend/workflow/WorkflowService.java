package org.opentron.backend.workflow;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WorkflowService {

    public List<Map<String, Object>> listWorkflows() {
        return List.of(
                Map.of(
                        "id", "example_workflow",
                        "name", "Example Workflow",
                        "description", "A placeholder workflow definition for migration support",
                        "status", "available"
                )
        );
    }

    public Map<String, Object> runWorkflow(WorkflowRunRequest request) {
        String workflowId = request.getWorkflow() != null ? request.getWorkflow() : "unknown";
        String runId = String.format("workflow-run-%d", System.currentTimeMillis());

        Map<String, Object> workflow = Map.of(
                "id", workflowId,
                "name", "Migrated Workflow",
                "description", "A native Java workflow execution placeholder",
                "status", "running"
        );

        return Map.of(
                "workflow", workflow,
                "input", request.getInput(),
                "run_id", runId,
                "status", "started",
                "started_at", java.time.Instant.now().toString(),
                "message", "Workflow execution initiated successfully"
        );
    }
}
