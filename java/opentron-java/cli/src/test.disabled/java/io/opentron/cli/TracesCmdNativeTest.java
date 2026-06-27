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

public class TracesCmdNativeTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/v1/traces", new TracesHandler());
        server.start();
        Utils.setBackendUrl("http://127.0.0.1:" + port);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        Utils.resetBackendUrl();
    }

    @Test
    void testListTracesUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            TracesCmd.main(new String[]{"list"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\"traces\""));
            assertTrue(output.contains("trace-1"));
            assertTrue(output.contains("example-event"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testGetTraceUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            TracesCmd.main(new String[]{"get", "trace-1"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\"id\": \"trace-1\""));
            assertTrue(output.contains("\"kind\": \"example-event\""));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testCreateTraceUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            TracesCmd.main(new String[]{"create", "event", "{\"key\": \"value\"}"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\"kind\": \"event\""));
            assertTrue(output.contains("\"payload\""));
            assertTrue(output.contains("\"key\": \"value\""));
        } finally {
            System.setOut(originalOut);
        }
    }

    private static class TracesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/v1/traces".equals(path) && "GET".equals(method)) {
                handleList(exchange);
                return;
            }
            if (path.startsWith("/v1/traces/") && "GET".equals(method)) {
                handleGet(exchange, path.substring("/v1/traces/".length()));
                return;
            }
            if ("/v1/traces".equals(path) && "POST".equals(method)) {
                handleCreate(exchange);
                return;
            }
            sendResponse(exchange, 404, Map.of("error", "not_found"));
        }

        private void handleList(HttpExchange exchange) throws IOException {
            Map<String, Object> trace = Map.of(
                    "id", "trace-1",
                    "kind", "example-event",
                    "payload", Map.of("foo", "bar")
            );
            sendResponse(exchange, 200, Map.of("traces", List.of(trace)));
        }

        private void handleGet(HttpExchange exchange, String traceId) throws IOException {
            if (!"trace-1".equals(traceId)) {
                sendResponse(exchange, 404, Map.of("error", "not_found"));
                return;
            }
            Map<String, Object> trace = Map.of(
                    "id", "trace-1",
                    "kind", "example-event",
                    "payload", Map.of("foo", "bar")
            );
            sendResponse(exchange, 200, trace);
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            Map<?, ?> request = GSON.fromJson(body, Map.class);
            assertEquals("event", request.get("kind"));
            assertTrue(((Map<?, ?>) request.get("payload")).containsKey("key"));

            Map<String, Object> result = Map.of(
                    "id", "trace-2",
                    "kind", request.get("kind"),
                    "payload", request.get("payload")
            );
            sendResponse(exchange, 200, result);
        }

        private String readBody(HttpExchange exchange) throws IOException {
            try (InputStream input = exchange.getRequestBody()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
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
