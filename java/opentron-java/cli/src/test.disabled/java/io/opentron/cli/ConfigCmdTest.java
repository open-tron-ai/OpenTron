package io.opentron.cli;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCmdTest {

    @Test
    void testShowJsonFromConfigFile() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");
        Files.writeString(configFile, "[intelligence]\ndefault_model = \"gpt-4.1\"\n" +
                "temperature = 0.5\n" +
                "[agent]\ndefault_agent = \"alpha\"\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            ConfigCmd.showJson(configFile);
            String jsonOutput = out.toString();
            assertTrue(jsonOutput.contains("\"default_model\""));
            assertTrue(jsonOutput.contains("gpt-4.1"));
            assertTrue(jsonOutput.contains("0.5"));
            assertTrue(jsonOutput.contains("alpha"));
        } finally {
            System.setOut(originalOut);
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testSetConfigValueCreatesFileAndSetsKey() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            ConfigCmd.setConfigValue(configFile, "engine.default", "gpt-4.1");
            assertTrue(Files.exists(configFile));
            String content = Files.readString(configFile);
            assertTrue(content.contains("engine.default = \"gpt-4.1\""));
            assertTrue(out.toString().contains("Set engine.default = gpt-4.1"));
        } finally {
            System.setOut(originalOut);
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testShowLoadedBasicReportsMissingFile() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            ConfigCmd.showLoadedBasic(configFile);
            String output = out.toString();
            assertTrue(output.contains("Config file not found."));
        } finally {
            System.setOut(originalOut);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testShowTomlGeneratesDefaultTemplateWhenMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            ConfigCmd.showToml(configFile);
            String output = out.toString();
            assertTrue(output.contains("Default configuration template:"));
            assertTrue(output.contains("[engine]"));
            assertTrue(output.contains("[intelligence]"));
            assertTrue(output.contains("[agent]"));
        } finally {
            System.setOut(originalOut);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testSetConfigValueCreatesNestedTable() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-config-test");
        Path configFile = tempDir.resolve("config.toml");

        // Start with a simple file containing a top-level table
        Files.writeString(configFile, "[engine]\ndefault = \"ollama\"\n\n");

        try {
            ConfigCmd.setConfigValue(configFile, "agent.settings.max_turns", "20");
            String content = Files.readString(configFile);
            assertTrue(content.contains("[agent.settings]"));
            assertTrue(content.contains("max_turns = 20") || content.contains("max_turns = \"20\""));
        } finally {
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        }
    }
}
