package io.opentron.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.opentron.core.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LearningCmdTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/v1/learning/stats", new JsonHandler(Map.of("status", "ok", "active_agents", 3)));
        server.createContext("/v1/learning/policy", new JsonHandler(Map.of("policy", Map.of("exploration_rate", 0.1))));
        server.start();
        Utils.setBackendUrl("http://127.0.0.1:" + port);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        Utils.resetBackendUrl();
    }

    @Test
    void testShowStatsUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            LearningCmd.main(new String[]{"stats"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\"status\""));
            assertTrue(output.contains("ok"));
            assertTrue(output.contains("\"active_agents\""));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testShowPolicyUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            LearningCmd.main(new String[]{"policy"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\"policy\""));
            assertTrue(output.contains("exploration_rate"));
            assertTrue(output.contains("0.1"));
        } finally {
            System.setOut(originalOut);
        }
    }

    private static class JsonHandler implements HttpHandler {
        private final Map<String, Object> payload;

        JsonHandler(Map<String, Object> payload) {
            this.payload = payload;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }
}
