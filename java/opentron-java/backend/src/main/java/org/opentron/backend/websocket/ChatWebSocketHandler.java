package org.opentron.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentron.backend.util.EngineRouting;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.util.concurrent.Executors;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebClient webClient;
    private final EngineRouting engineRouting;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatWebSocketHandler(WebClient webClient, EngineRouting engineRouting) {
        this.webClient = webClient;
        this.engineRouting = engineRouting;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Parse client JSON and proxy to engine /v1/chat/completions with stream=true
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                JsonNode req = mapper.readTree(message.getPayload());
                // Build payload: copy incoming JSON and ensure stream=true
                ((com.fasterxml.jackson.databind.node.ObjectNode) req).put("stream", true);
                // Call engine and stream SSE chunks
                webClient.post()
                        .uri(engineRouting.translateRequestPath("/v1/chat/completions"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(req.toString())
                        .retrieve()
                        .bodyToFlux(String.class)
                        .subscribe(chunk -> {
                            try {
                                // Extract `data: ` lines if present
                                String[] lines = chunk.split("\n");
                                for (String ln : lines) {
                                    if (ln.startsWith("data: ")) {
                                        String data = ln.substring(6).trim();
                                        if ("[DONE]".equals(data)) {
                                            session.sendMessage(new TextMessage("{\"type\":\"done\"}"));
                                        } else {
                                            // Try to parse and extract delta.content
                                            try {
                                                JsonNode j = mapper.readTree(data);
                                                String content = null;
                                                JsonNode choices = j.path("choices");
                                                if (choices.isArray() && choices.size() > 0) {
                                                    JsonNode delta = choices.get(0).path("delta");
                                                    if (delta.has("content")) content = delta.get("content").asText();
                                                }
                                                if (content != null) {
                                                    String out = mapper.createObjectNode()
                                                            .put("type", "chunk")
                                                            .put("content", content)
                                                            .toString();
                                                    session.sendMessage(new TextMessage(out));
                                                }
                                            } catch (Exception ex) {
                                                // Forward raw as chunk
                                                session.sendMessage(new TextMessage(mapper.createObjectNode().put("type","chunk").put("content", data).toString()));
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                try {
                                    session.sendMessage(new TextMessage("{\"type\":\"error\",\"detail\":\"" + ex.getMessage() + "\"}"));
                                } catch (Exception ignore) {
                                }
                            }
                        }, err -> {
                            try {
                                session.sendMessage(new TextMessage("{\"type\":\"error\",\"detail\":\"" + err.getMessage() + "\"}"));
                            } catch (Exception ignore) {
                            }
                        });
            } catch (Exception e) {
                try {
                    session.sendMessage(new TextMessage("{\"type\":\"error\",\"detail\":\"" + e.getMessage() + "\"}"));
                } catch (Exception ignore) {
                }
            }
        });
    }
}
