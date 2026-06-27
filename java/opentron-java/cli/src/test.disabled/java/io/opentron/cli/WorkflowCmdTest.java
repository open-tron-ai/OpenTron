package io.opentron.cli;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowCmdTest {

    @Test
    void testBuildRunRequestIncludesWorkflowAndInput() {
        Map<String, Object> request = WorkflowCmd.buildRunRequest("example_workflow", "hello world");
        assertEquals("example_workflow", request.get("workflow"));
        assertEquals("hello world", request.get("input"));
    }

    @Test
    void testBuildRunRequestOmitsInputWhenNull() {
        Map<String, Object> request = WorkflowCmd.buildRunRequest("example_workflow", null);
        assertEquals("example_workflow", request.get("workflow"));
        assertFalse(request.containsKey("input"));
    }

    @Test
    void testExtractInputReturnsValueWhenProvided() {
        String input = WorkflowCmd.extractInput(new String[]{"run", "example_workflow", "--input", "hello world"});
        assertEquals("hello world", input);
    }

    @Test
    void testExtractInputReturnsNullWhenNoInput() {
        String input = WorkflowCmd.extractInput(new String[]{"run", "example_workflow"});
        assertNull(input);
    }

    @Test
    void testPrettyFormatsValidJson() {
        String pretty = WorkflowCmd.pretty("{\"workflow\":\"example\",\"status\":\"started\"}");
        assertTrue(pretty.contains("\"workflow\""));
        assertTrue(pretty.contains("\"status\""));
    }
}
