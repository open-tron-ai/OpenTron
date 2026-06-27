package io.opentron.cli;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class InitCmdTest {

    @Test
    void testInitWritesDefaultConfig() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-init-test");
        Path configFile = tempDir.resolve("config.toml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            int rc = InitCmd.run(new String[]{"--path", configFile.toString(), "--force"});
            assertEquals(0, rc);
            assertTrue(Files.exists(configFile));
            String content = Files.readString(configFile);
            assertTrue(content.contains("[engine]"));
            assertTrue(content.contains("[intelligence]"));
            assertTrue(content.contains("[agent]"));
        } finally {
            System.setOut(originalOut);
            cleanRecursively(tempDir);
        }
    }

    @Test
    void testInitCreatesSupportFilesAndPrintsNextSteps() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-init-support-test");
        Path configFile = tempDir.resolve("config.toml");
        Path soulFile = tempDir.resolve("SOUL.md");
        Path memoryFile = tempDir.resolve("MEMORY.md");
        Path userFile = tempDir.resolve("USER.md");
        Path skillsDir = tempDir.resolve("skills");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            int rc = InitCmd.run(new String[]{"--path", configFile.toString(), "--force"});
            assertEquals(0, rc);
            assertTrue(Files.exists(configFile));
            assertTrue(Files.exists(soulFile));
            assertTrue(Files.exists(memoryFile));
            assertTrue(Files.exists(userFile));
            assertTrue(Files.isDirectory(skillsDir));

            String output = out.toString();
            assertTrue(output.contains("Config written to"));
            assertTrue(output.contains("Next steps:"));
            assertTrue(output.contains("Tron ask"));
            assertTrue(output.contains("Tron doctor"));
        } finally {
            System.setOut(originalOut);
            cleanRecursively(tempDir);
        }
    }

    private static void cleanRecursively(Path path) throws Exception {
        if (Files.notExists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception e) {
                        // ignore cleanup failures
                    }
                });
    }
}
