package io.opentron.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ToolCmdInspectTest {

    @Test
    void testPrintToolSummaryIncludesConfiguredAndSource() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", "browser");
            tool.put("category", "tool");
            tool.put("description", "Browser automation tool");
            tool.put("source", "builtin");
            tool.put("configured", true);
            tool.put("requires_credentials", false);
            tool.put("credential_keys", List.of());
            tool.put("capabilities", List.of("navigate", "click"));
            tool.put("parameters", Map.of(
                    "url", Map.of("type", "string", "description", "Target URL"),
                    "action", Map.of("type", "string", "description", "What to do")
            ));

            ToolCmd.printToolSummary(tool);
            String output = out.toString();
            assertTrue(output.contains("Name:"));
            assertTrue(output.contains("Category:"));
            assertTrue(output.contains("Configured:"));
            assertTrue(output.contains("Source:"));
            assertTrue(output.contains("Capabilities:"));
            assertTrue(output.contains("navigate"));
            assertTrue(output.contains("Parameter keys:"));
            assertTrue(output.contains("action, url") || output.contains("url, action"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testSafeStringNullReturnsEmpty() {
        assertEquals("", ToolCmd.safeString(null));
    }
}
