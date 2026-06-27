package io.opentron.cli;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigNestedSetTest {

    @Test
    void testSetNestedConfigCreatesMissingAncestorsAndPreservesValidToml() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        try {
            Files.writeString(configFile, "[engine]\ndefault = \"ollama\"\n\n");
            ConfigCmd.setConfigValue(configFile, "agent.settings.max_turns", "20");

            String content = Files.readString(configFile);
            assertTrue(content.contains("[agent.settings]"));
            assertTrue(content.contains("max_turns = 20"));
            assertDoesNotThrow(() -> {
                TomlParseResult result = Toml.parse(content);
                assertFalse(result.hasErrors());
            });
        } finally {
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testSetConfigRollbackOnInvalidTomlUpdate() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        String initial = "[engine]\ndefault = \"ollama\"\n";
        Files.writeString(configFile, initial);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(err));

        try {
            ConfigCmd.setConfigValue(configFile, "agent[bad].settings.max_turns", "20");
            String content = Files.readString(configFile);
            assertEquals(initial, content);
            assertTrue(err.toString().contains("invalid TOML") || err.toString().contains("Failed to set config"));
        } finally {
            System.setErr(originalErr);
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        }
    }
}
