package io.opentron.cli;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ToolCmdTest {

    @Test
    void testPrintUsageIncludesHelp() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            ToolCmd.printUsage();
            String output = out.toString();
            assertTrue(output.contains("help"));
            assertTrue(output.contains("list"));
            assertTrue(output.contains("inspect"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testSafeStringNullReturnsEmpty() {
        assertEquals("", ToolCmd.safeString(null));
        assertEquals("value", ToolCmd.safeString("value"));
    }
}
