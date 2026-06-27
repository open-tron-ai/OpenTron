package io.opentron.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelsCmdInspectTest {

    @Test
    void testPrintUsageIncludesInspect() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            ChannelsCmd.printUsage();
            String output = out.toString();
            assertTrue(output.contains("inspect"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testFilterChannelToolsReturnsOnlyChannels() {
        Map<String, Object> browser = new HashMap<>();
        browser.put("name", "browser");
        browser.put("category", "tool");

        Map<String, Object> slack = new HashMap<>();
        slack.put("name", "slack");
        slack.put("category", "channel");

        assertEquals(1, ChannelsCmd.filterChannelTools(java.util.List.of(browser, slack)).size());
        assertEquals("slack", ChannelsCmd.filterChannelTools(java.util.List.of(browser, slack)).get(0).get("name"));
    }
}
