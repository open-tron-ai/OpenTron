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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ComposeCmdNativeTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/v1/compose", new ComposeListHandler());
        server.createContext("/v1/compose/run", new ComposeRunHandler());
        server.start();
        Utils.setBackendUrl("http://127.0.0.1:" + port);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        Utils.resetBackendUrl();
    }

    @Test
    void testListComposeUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            ComposeCmd.main(new String[] {"list"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("hello_world"), "Output should include composition name");
            assertTrue(output.contains("discrete"), "Output should include composition kind");
            assertTrue(output.contains("Total: 1 composition(s)"), "Output should show total count");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunComposeUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            ComposeCmd.main(new String[] {"run", "hello_world", "hello", "compose"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Composition run initiated:"), "Should print run initiation message");
            assertTrue(output.contains("ID:"), "Should print run ID");
            assertTrue(output.contains("Status: started"), "Should print started status");
        } finally {
            System.setOut(originalOut);
        }
    }

    private static class ComposeListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "method_not_allowed"));
                return;
            }
            List<Map<String, Object>> compositions = List.of(
                Map.of(
                    "name", "hello_world",
                    "kind", "discrete",
                    "model", "gpt-4",
                    "agent_type", "direct",
                    "tools", List.of("search", "browser"),
                    "description", "Example composition placeholder"
                )
            );
            sendResponse(exchange, 200, compositions);
        }
    }

    private static class ComposeRunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "method_not_allowed"));
                return;
            }
            String body = readBody(exchange);
            Map<?, ?> request = GSON.fromJson(body, Map.class);
            assertEquals("hello_world", request.get("name"));
            assertEquals("hello compose", request.get("query"));

            Map<String, Object> response = Map.of(
                "id", "compose-run-1",
                "name", request.get("name"),
                "status", "started",
                "message", "Composition run initiated successfully",
                "result", "Placeholder result"
            );
            sendResponse(exchange, 200, response);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
