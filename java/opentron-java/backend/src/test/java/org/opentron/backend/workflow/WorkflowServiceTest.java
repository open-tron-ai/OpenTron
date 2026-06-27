package org.opentron.backend.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowServiceTest {

    @Test
    public void testListWorkflowsReturnsExampleWorkflow() {
        WorkflowService service = new WorkflowService();
        List<Map<String, Object>> workflows = service.listWorkflows();

        assertNotNull(workflows);
        assertEquals(1, workflows.size());

        Map<String, Object> workflow = workflows.get(0);
        assertEquals("example_workflow", workflow.get("id"));
        assertEquals("Example Workflow", workflow.get("name"));
        assertEquals("available", workflow.get("status"));
    }

    @Test
    public void testRunWorkflowReturnsStartedMetadata() {
        WorkflowService service = new WorkflowService();
        WorkflowRunRequest request = new WorkflowRunRequest();
        request.setWorkflow("example_workflow");
        request.setInput("hello world");

        Map<String, Object> result = service.runWorkflow(request);

        assertNotNull(result);
        assertEquals("started", result.get("status"));
        assertEquals("Workflow execution initiated successfully", result.get("message"));
        assertNotNull(result.get("run_id"));
        assertNotNull(result.get("started_at"));

        assertTrue(result.get("run_id").toString().startsWith("workflow-run-"));

        assertTrue(result.containsKey("workflow"));
        assertTrue(result.get("workflow") instanceof Map<?, ?>);
        Map<?, ?> workflow = (Map<?, ?>) result.get("workflow");
        assertEquals("example_workflow", workflow.get("id"));
        assertEquals("running", workflow.get("status"));
    }

    @Test
    public void testRunWorkflowUsesUnknownWhenWorkflowMissing() {
        WorkflowService service = new WorkflowService();
        WorkflowRunRequest request = new WorkflowRunRequest();
        request.setInput("hello world");

        Map<String, Object> result = service.runWorkflow(request);

        assertNotNull(result);
        assertTrue(result.get("workflow") instanceof Map<?, ?>);
        Map<?, ?> workflow = (Map<?, ?>) result.get("workflow");
        assertEquals("unknown", workflow.get("id"));
    }
}
