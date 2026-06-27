package io.opentron.cli;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ScanCmdTest {

    @Test
    void testHelpPrintsUsage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            int rc = ScanCmd.run(new String[]{"--help"});
            assertEquals(0, rc);
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Usage: tron scan"));
            assertTrue(output.contains("--quick"));
            assertTrue(output.contains("--json"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testJsonOutputIsValidArray() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            int rc = ScanCmd.run(new String[]{"--json"});
            assertEquals(0, rc);
            String json = out.toString(StandardCharsets.UTF_8).trim();
            assertFalse(json.isEmpty());
            JsonArray array = new Gson().fromJson(json, JsonArray.class);
            assertNotNull(array);
        } finally {
            System.setOut(originalOut);
        }
    }
}
