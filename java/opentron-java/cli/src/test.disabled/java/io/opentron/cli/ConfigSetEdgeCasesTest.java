package io.opentron.cli;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSetEdgeCasesTest {

    @Test
    void testInvalidTomlRollbackOnBadParentName() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        // Write an initial valid config
        String initial = "[engine]\ndefault = \"ollama\"\n";
        Files.writeString(configFile, initial);

        // Capture stderr to suppress error output in test logs
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(err));

        try {
            // Use a dotted key with a bracket which will lead to invalid TOML header creation
            ConfigCmd.setConfigValue(configFile, "agent.settings[bad].max", "10");

            // Ensure the original content remains unchanged (rollback occurred)
            String content = Files.readString(configFile);
            assertEquals(initial, content);
        } finally {
            System.setErr(originalErr);
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testValueCoercionPrimitivesAndEscaping() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        try {
            // Numbers and booleans should be unquoted
            ConfigCmd.setConfigValue(configFile, "values.int", "42");
            ConfigCmd.setConfigValue(configFile, "values.bool", "true");
            ConfigCmd.setConfigValue(configFile, "values.float", "3.14");

            // String with quotes and backslashes should be escaped and quoted
            ConfigCmd.setConfigValue(configFile, "values.str", "a\"b\\c");

            String content = Files.readString(configFile);
            assertTrue(content.contains("values.int = 42"));
            assertTrue(content.contains("values.bool = true"));
            assertTrue(content.contains("values.float = 3.14"));
            assertTrue(content.contains("values.str = \"a\\\"b\\\\c\""));
        } finally {
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        }
    }
}
