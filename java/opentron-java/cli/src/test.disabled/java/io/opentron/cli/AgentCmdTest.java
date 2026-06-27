package io.opentron.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.opentron.core.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AgentCmdTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private int port;
    private final Map<String, Map<String, Object>> agents = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/v1/agents", new AgentsHandler());
        server.start();
        Utils.setBackendUrl("http://127.0.0.1:" + port);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        Utils.resetBackendUrl();
        agents.clear();
        nextId.set(1);
    }

    @Test
    void testCreateListMessageAndKill() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        try {
            AgentCmd.main(new String[]{"create", "test-agent"});
            String createOutput = output.toString(StandardCharsets.UTF_8);
            assertTrue(createOutput.contains("\"name\": \"test-agent\""), "Create output should contain agent name");
            output.reset();

            AgentCmd.main(new String[]{"list"});
            String listOutput = output.toString(StandardCharsets.UTF_8);
            assertTrue(listOutput.contains("\"name\": \"test-agent\""), "List output should show created agent");
            output.reset();

            String createdAgentId = agents.keySet().iterator().next();
            AgentCmd.main(new String[]{"message", createdAgentId, "hello", "world"});
            String messageOutput = output.toString(StandardCharsets.UTF_8);
            assertTrue(messageOutput.contains("\"messages\""), "Message API should return updated agent object");
            assertTrue(messageOutput.contains("hello world"), "Message output should contain sent content");
            output.reset();

            AgentCmd.main(new String[]{"kill", createdAgentId});
            String killOutput = output.toString(StandardCharsets.UTF_8);
            assertTrue(killOutput.contains("\"id\": \"" + createdAgentId + "\""), "Kill output should confirm agent id");
        } finally {
            System.setOut(originalOut);
        }
    }

    private class AgentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                if ("/v1/agents".equals(path)) {
                    if ("GET".equals(method)) {
                        handleList(exchange);
                        return;
                    }
                    if ("POST".equals(method)) {
                        handleCreate(exchange);
                        return;
                    }
                }

                if (path.startsWith("/v1/agents/")) {
                    String remainder = path.substring("/v1/agents/".length());
                    if (remainder.endsWith("/message") && "POST".
                            equals(method)) {
                        String agentId = remainder.substring(0, remainder.length() - "/message".length());
                        handleMessage(exchange, agentId);
                        return;
                    }
                    if ("DELETE".equals(method)) {
                        handleDelete(exchange, remainder);
                        return;
                    }
                }

                sendResponse(exchange, 404, Map.of("error", "not_found"));
            } catch (Exception e) {
                sendResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }

        private void handleList(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, new ArrayList<>(agents.values()));
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            Map<?, ?> request = body.isBlank() ? Map.of() : GSON.fromJson(body, Map.class);
            String name = request.containsKey("name") ? String.valueOf(request.get("name")) : "agent-" + nextId.get();
            String id = String.valueOf(nextId.getAndIncrement());
            Map<String, Object> agent = new HashMap<>();
            agent.put("id", id);
            agent.put("name", name);
            agent.put("status", "running");
            agent.put("messages", new ArrayList<>());
            agents.put(id, agent);
            sendResponse(exchange, 200, agent);
        }

        private void handleDelete(HttpExchange exchange, String agentId) throws IOException {
            if (agents.remove(agentId) != null) {
                sendResponse(exchange, 200, Map.of("id", agentId));
            } else {
                sendResponse(exchange, 404, Map.of("error", "not_found"));
            }
        }

        private void handleMessage(HttpExchange exchange, String agentId) throws IOException {
            Map<String, Object> agent = (Map<String, Object>) agents.get(agentId);
            if (agent == null) {
                sendResponse(exchange, 404, Map.of("error", "not_found"));
                return;
            }
            String body = readBody(exchange);
            Map<?, ?> request = body.isBlank() ? Map.of() : GSON.fromJson(body, Map.class);
            String content = request.containsKey("content") ? String.valueOf(request.get("content")) : "";
            List<String> messages = (List<String>) agent.get("messages");
            messages.add(content);
            sendResponse(exchange, 200, agent);
        }

        private String readBody(HttpExchange exchange) throws IOException {
            try (InputStream input = exchange.getRequestBody()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
            byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }
}
