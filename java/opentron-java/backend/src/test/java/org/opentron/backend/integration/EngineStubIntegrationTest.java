package org.opentron.backend.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opentron.backend.OpentronBackendApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = OpentronBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EngineStubIntegrationTest {

    private static HttpServer engineStub;

    @BeforeAll
    public void startStub() throws Exception {
        startEngineStubIfNeeded();
    }

    @AfterAll
    public void stopStub() {
        if (engineStub != null) engineStub.stop(0);
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        try {
            startEngineStubIfNeeded();
            int port = engineStub.getAddress().getPort();
            registry.add("engine.host", () -> "http://127.0.0.1:" + port);
            registry.add("engine.type", () -> "ollama");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void startEngineStubIfNeeded() throws IOException {
        if (engineStub == null) {
            engineStub = HttpServer.create(new InetSocketAddress(0), 0);
            engineStub.createContext("/v1/models", new ModelsHandler());
            engineStub.createContext("/v1/chat/completions", new ChatHandler());
            engineStub.createContext("/api/tags", new ModelsHandler());
            engineStub.createContext("/api/chat", new ChatHandler());
            engineStub.setExecutor(r -> new Thread(r));
            engineStub.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (engineStub != null) engineStub.stop(0);
            }));
        }
    }

    @Value("${local.server.port}")
    int localPort;

    @Test
    @Disabled
    public void testModelsProxy() {
        WebClient client = WebClient.builder().baseUrl("http://127.0.0.1:" + localPort).build();
        String res = client.get().uri("/v1/models").retrieve().bodyToMono(String.class).block(Duration.ofSeconds(5));
        assertNotNull(res);
        assertTrue(res.contains("test-model"));
    }

    @Test
    @Disabled
    public void testChatSSEStream() {
        WebClient client = WebClient.builder().baseUrl("http://127.0.0.1:" + localPort).build();
        String payload = "{\"model\":\"test\",\"messages\":[],\"stream\":true}";
        List<String> parts = client.post().uri("/v1/chat/completions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block(Duration.ofSeconds(5));
        assertNotNull(parts);
        boolean sawHello = parts.stream().anyMatch(s -> s.contains("Hello-from-engine"));
        assertTrue(sawHello);
    }

    @Test
    @Disabled
    public void testWebSocketRelay() throws Exception {
        WebSocketClient wsClient = new ReactorNettyWebSocketClient();
        String wsUrl = "ws://127.0.0.1:" + localPort + "/v1/chat/stream";
        List<String> received = new ArrayList<>();
        Mono<Void> sessionMono = wsClient.execute(URI.create(wsUrl), session -> {
            Mono<Void> send = session.send(Mono.just(session.textMessage("{\"model\":\"test\",\"messages\":[],\"stream\":true}")));
            Mono<Void> receive = session.receive().map(WebSocketMessage::getPayloadAsText).doOnNext(received::add).then();
            return Mono.when(send, receive).then();
        });
        sessionMono.block(Duration.ofSeconds(10));
        boolean sawChunk = received.stream().anyMatch(s -> s.contains("Hello-from-engine"));
        assertTrue(sawChunk);
    }

    static class ModelsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("[EngineStub] ModelsHandler invoked: " + exchange.getRequestURI() + " " + exchange.getRequestMethod());
            String body = "{\"data\":[{\"id\":\"test-model\"}]}";
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }
    }

    static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("[EngineStub] ChatHandler invoked: " + exchange.getRequestURI() + " " + exchange.getRequestMethod());
            // read body (ignored)
            try (InputStream is = exchange.getRequestBody()) { while (is.read() != -1) ; }
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            // send one token event then DONE
            String part = "data: {\"choices\":[{\"delta\":{\"content\":\"Hello-from-engine\"}}]}\n\n";
            os.write(part.getBytes(StandardCharsets.UTF_8));
            os.flush();
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            String done = "data: [DONE]\n\n";
            os.write(done.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
        }
    }
}
