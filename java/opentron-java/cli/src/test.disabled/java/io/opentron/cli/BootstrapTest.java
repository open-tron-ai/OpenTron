package io.opentron.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class BootstrapTest {

    private final String originalUserHome = System.getProperty("user.home");

    @AfterEach
    void restoreUserHome() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void testBootstrapWritesConfigAndSupportFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-bootstrap-test");
        System.setProperty("user.home", tempDir.toString());

        int rc = Bootstrap.run(new String[]{"--write-config", "--engine", "ollama", "--model", "qwen3.5:2b"});
        assertEquals(0, rc);

        Path configFile = tempDir.resolve(".OpenTron").resolve("config.toml");
        assertTrue(Files.exists(configFile));
        String content = Files.readString(configFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("[engine]"));
        assertTrue(content.contains("[intelligence]"));
        assertTrue(content.contains("default_model = \"qwen3.5:2b\""));

        Path soulFile = tempDir.resolve(".OpenTron").resolve("SOUL.md");
        Path memoryFile = tempDir.resolve(".OpenTron").resolve("MEMORY.md");
        Path userFile = tempDir.resolve(".OpenTron").resolve("USER.md");
        Path skillsDir = tempDir.resolve(".OpenTron").resolve("skills");

        assertTrue(Files.exists(soulFile));
        assertTrue(Files.exists(memoryFile));
        assertTrue(Files.exists(userFile));
        assertTrue(Files.isDirectory(skillsDir));

        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
    }

    @Test
    void testBootstrapPreferCloudOverridesEngine() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-bootstrap-cloud-test");
        System.setProperty("user.home", tempDir.toString());

        int rc = Bootstrap.run(
                new String[]{"--write-config", "--prefer-cloud-when-available", "--engine", "ollama", "--model", "qwen3.5:2b"},
                java.util.Map.of("ANTHROPIC_API_KEY", "sk-ant-test")
        );
        assertEquals(0, rc);

        Path configFile = tempDir.resolve(".OpenTron").resolve("config.toml");
        assertTrue(Files.exists(configFile));
        String content = Files.readString(configFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("default = \"cloud\""));
        assertTrue(content.contains("provider = \"anthropic\""));

        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
    }
}
