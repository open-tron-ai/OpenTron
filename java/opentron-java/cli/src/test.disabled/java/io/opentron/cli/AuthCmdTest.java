package io.opentron.cli;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AuthCmdTest {

    @Test
    void testCreateKeyWritesKeyToConfig() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-auth-test");
        Path config = tempDir.resolve("config.toml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            AuthCmd.createKey(config);
            assertTrue(Files.exists(config));
            String content = Files.readString(config);
            assertTrue(content.contains("[server.auth]"));
            assertTrue(content.contains("api_key = \""));
            assertTrue(out.toString().contains("API key generated:"));
        } finally {
            System.setOut(originalOut);
            Files.deleteIfExists(config);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testRevokeKeyClearsApiKey() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-auth-test");
        Path config = tempDir.resolve("config.toml");
        Files.writeString(config, "[server.auth]\napi_key = \"secret\"\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            AuthCmd.revokeKey(config);
            String content = Files.readString(config);
            assertTrue(content.contains("api_key = \"\""));
            assertTrue(out.toString().contains("API key revoked."));
        } finally {
            System.setOut(originalOut);
            Files.deleteIfExists(config);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testRevokeKeyNoConfig() throws Exception {
        Path tempDir = Files.createTempDirectory("opentron-auth-test");
        Path config = tempDir.resolve("config.toml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            AuthCmd.revokeKey(config);
            assertTrue(out.toString().contains("No config file found."));
        } finally {
            System.setOut(originalOut);
            Files.deleteIfExists(tempDir);
        }
    }
}
