package io.opentron.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ToolCmdRichMetadataTest {

    @Test
    void testPrintToolSummaryIncludesDocumentationAndExamples() {
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
                    "action", Map.of("type", "string", "description", "Action to perform")
            ));
            tool.put("documentation_url", "https://docs.example.com/browser");
            tool.put("examples", List.of("browser navigate url=https://example.com"));

            ToolCmd.printToolSummary(tool);
            String output = out.toString();
            assertTrue(output.contains("Documentation_url:"));
            assertTrue(output.contains("Examples:"));
            assertTrue(output.contains("Parameter keys:"));
            assertTrue(output.contains("url, action") || output.contains("action, url"));
            assertTrue(output.contains("navigate, click") || output.contains("click, navigate"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
