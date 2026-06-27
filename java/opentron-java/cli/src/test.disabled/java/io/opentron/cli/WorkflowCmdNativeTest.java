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

public class WorkflowCmdNativeTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/v1/workflow", new WorkflowListHandler());
        server.createContext("/v1/workflow/run", new WorkflowRunHandler());
        server.start();
        Utils.setBackendUrl("http://127.0.0.1:" + port);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        Utils.resetBackendUrl();
    }

    @Test
    void testListWorkflowsUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            WorkflowCmd.main(new String[]{"list"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("example_workflow"), "Output should include workflow ID");
            assertTrue(output.contains("Example Workflow"), "Output should include workflow name");
            assertTrue(output.contains("Total: 1 workflow(s)"), "Output should show total count");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunWorkflowUsesNativeBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            WorkflowCmd.main(new String[]{"run", "example_workflow", "--input", "hello world"});
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("workflow-run-"), "Output should include run id");
            assertTrue(output.contains("Workflow execution initiated successfully"), "Output should include success message");
            assertTrue(output.contains("hello world"), "Output should reflect provided input");
        } finally {
            System.setOut(originalOut);
        }
    }

    private static class WorkflowListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "method_not_allowed"));
                return;
            }
            List<Map<String, Object>> workflows = List.of(
                    Map.of(
                            "id", "example_workflow",
                            "name", "Example Workflow",
                            "status", "active",
                            "description", "A placeholder workflow"
                    )
            );
            sendResponse(exchange, 200, workflows);
        }
    }

    private static class WorkflowRunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "method_not_allowed"));
                return;
            }
            String body = readBody(exchange);
            Map<?, ?> request = GSON.fromJson(body, Map.class);
            assertEquals("example_workflow", request.get("workflow"));
            assertEquals("hello world", request.get("input"));

            Map<String, Object> response = Map.of(
                    "id", "workflow-run-1",
                    "workflow", request,
                    "message", "Workflow execution initiated successfully"
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
