package org.opentron.backend.controllers;

import java.util.Map;
import java.util.List;

import org.opentron.backend.util.EngineRouting;
import org.opentron.backend.util.OllamaCliService;
import org.opentron.backend.util.ResilienceUtil;
import org.springframework.core.io.buffer.DataBuffer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final WebClient webClient;
    private final EngineRouting engineRouting;
    private final OllamaCliService ollamaCliService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatController(WebClient webClient, EngineRouting engineRouting, OllamaCliService ollamaCliService) {
        this.webClient = webClient;
        this.engineRouting = engineRouting;
        this.ollamaCliService = ollamaCliService;
    }

    @PostMapping(value = "/completions")
    public Mono<ResponseEntity<?>> completionsStream(HttpServletRequest servletRequest, @RequestBody Map<String, Object> payload) {
        servletRequest.setAttribute("org.apache.catalina.ASYNC_TIMEOUT", -1L);
        try {
            org.springframework.context.ApplicationContext ctx = org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext(servletRequest.getServletContext());
            org.opentron.backend.telemetry.TelemetryService ts = ctx.getBean(org.opentron.backend.telemetry.TelemetryService.class);
            if (ts != null) ts.recordRequest();
        } catch (Exception ignored) {}

        boolean stream = Boolean.TRUE.equals(payload.get("stream"));
        
        System.out.println("[ChatController] Request: model=" + payload.get("model") + " stream=" + stream);
        
        if (engineRouting.getEffectiveEngineType() == EngineRouting.EngineType.OLLAMA) {
            try {
                servletRequest.setAttribute("org.apache.catalina.ASYNC_TIMEOUT", 180000L);
            } catch (Exception e) {
                System.err.println("[ChatController] Could not set async timeout: " + e.getMessage());
            }
        }
        
        if (engineRouting.getEffectiveEngineType() == EngineRouting.EngineType.OLLAMA) {
            System.out.println("[ChatController] Using Ollama CLI");
            return handleOllamaCliChat(payload, stream);
        }

        String targetPath = engineRouting.translateRequestPath("/v1/chat/completions");
        Mono<Map<String, Object>> resolvedPayload = resolveOllamaModel(payload);
        WebClient.RequestBodySpec requestBuilder = webClient.post()
                .uri(targetPath)
                .contentType(MediaType.APPLICATION_JSON);

        if (stream) {
            requestBuilder = requestBuilder.accept(MediaType.TEXT_EVENT_STREAM);
        }

        HttpHeaders forwardHeaders = new HttpHeaders();
        if (servletRequest.getHeaderNames() != null) {
            for (String headerName : java.util.Collections.list(servletRequest.getHeaderNames())) {
                if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
                        || headerName.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)
                        || headerName.equalsIgnoreCase(HttpHeaders.HOST)
                        || headerName.equalsIgnoreCase(HttpHeaders.CONNECTION)) {
                    continue;
                }
                for (String headerValue : java.util.Collections.list(servletRequest.getHeaders(headerName))) {
                    forwardHeaders.add(headerName, headerValue);
                }
            }
        }
        requestBuilder.headers(http -> http.addAll(forwardHeaders));

        System.out.println("[ChatController] forwarding to engine " + targetPath + " stream=" + stream);

        WebClient.RequestBodySpec reqSpecBase = requestBuilder;

        Mono<ResponseEntity<?>> result = resolvedPayload.flatMap(resolved -> reqSpecBase.bodyValue(resolved).exchange())
            .flatMap(response -> buildResponseMono(response, stream))
            .onErrorResume(e -> {
                System.err.println("[ChatController] exchange error: " + e.getMessage());
                e.printStackTrace(System.err);
                return Mono.just(ResponseEntity.status(502).body((Object)"{\"error\":\"chat-failed\"}"));
            });

        if (stream) {
            return ResilienceUtil.withResilienceStreaming(result);
        } else {
            return ResilienceUtil.withResilienceNonStreaming(result);
        }
    }

    @SuppressWarnings("unchecked")
    private Mono<ResponseEntity<?>> handleOllamaCliChat(Map<String, Object> payload, boolean stream) {
        return ollamaCliService.chatCompletion(
            (String) payload.getOrDefault("model", "mistral"),
            (List<Map<String, String>>) payload.get("messages")
        )
        .<ResponseEntity<?>>map(result -> {
            try {
                if (stream) {
                    // Transform the non-streaming response into OpenAI streaming format with deltas
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                    if (choices == null || choices.isEmpty()) {
                        return ResponseEntity.status(500).body((Object)"{\"error\":\"no-choices\"}");
                    }
                    
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = message != null ? (String) message.get("content") : "";
                    
                    // Break content into word chunks and create streaming chunks
                    StringBuilder sseBuilder = new StringBuilder();
                    String[] words = content.split(" ");
                    
                    for (int i = 0; i < words.length; i++) {
                        Map<String, Object> chunk = new java.util.HashMap<>();
                        chunk.put("model", result.get("model"));
                        chunk.put("created", result.get("created"));
                        
                        List<Map<String, Object>> chunkChoices = new java.util.ArrayList<>();
                        Map<String, Object> chunkChoice = new java.util.HashMap<>();
                        chunkChoice.put("index", 0);
                        
                        Map<String, Object> delta = new java.util.HashMap<>();
                        if (i == 0) {
                            delta.put("role", "assistant");
                        }
                        delta.put("content", words[i] + (i < words.length - 1 ? " " : ""));
                        chunkChoice.put("delta", delta);
                        
                        if (i == words.length - 1) {
                            chunkChoice.put("finish_reason", "stop");
                        } else {
                            chunkChoice.put("finish_reason", null);
                        }
                        
                        chunkChoices.add(chunkChoice);
                        chunk.put("choices", chunkChoices);
                        
                        String json = objectMapper.writeValueAsString(chunk);
                        sseBuilder.append("data: ").append(json).append("\n\n");
                    }
                    
                    sseBuilder.append("data: [DONE]\n\n");
                    System.out.println("[ChatController] Transformed response to OpenAI streaming format");
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_EVENT_STREAM)
                            .body((Object)sseBuilder.toString());
                } else {
                    // Non-streaming: return as-is (already in OpenAI format with message field)
                    String json = objectMapper.writeValueAsString(result);
                    System.out.println("[ChatController] Returning non-streaming OpenAI format response");
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body((Object)json);
                }
            } catch (Exception e) {
                System.err.println("[ChatController] Serialization error: " + e.getMessage());
                e.printStackTrace(System.err);
                return ResponseEntity.status(500).body((Object)"{\"error\":\"serialization-failed\"}");
            }
        })
        .onErrorResume(e -> {
            System.err.println("[ChatController] Ollama CLI error: " + e.getMessage());
            e.printStackTrace(System.err);
            String errMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "unknown error";
            return Mono.<ResponseEntity<?>>just(ResponseEntity.status(502)
                    .body((Object)("{\"error\":\"ollama-cli-failed\",\"message\":\"" + errMsg + "\"}")));
        });
    }

    private Mono<ResponseEntity<?>> buildResponseMono(ClientResponse response, boolean stream) {
        HttpHeaders responseHeaders = new HttpHeaders();
        response.headers().asHttpHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH) &&
                !name.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                responseHeaders.put(name, values);
            }
        });

        if (stream) {
            System.out.println("[ChatController] Proxying streaming response from engine");
            Flux<DataBuffer> bodyFlux = response.bodyToFlux(DataBuffer.class);
            return Mono.just(
                ResponseEntity.status(response.statusCode())
                    .headers(responseHeaders)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(bodyFlux)
            );
        } else {
            System.out.println("[ChatController] Proxying non-streaming response from engine");
            return response.bodyToMono(String.class)
                    .map(bodyText -> {
                        return ResponseEntity.status(response.statusCode())
                                .headers(responseHeaders)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body((Object)bodyText);
                    });
        }
    }

    private Mono<Map<String, Object>> resolveOllamaModel(Map<String, Object> payload) {
        if (engineRouting.getEffectiveEngineType() != org.opentron.backend.util.EngineRouting.EngineType.OLLAMA) {
            return Mono.just(payload);
        }

        Object m = payload.get("model");
        String requestedModel = m == null ? null : String.valueOf(m);
        if (requestedModel == null || requestedModel.isBlank()) {
            return Mono.just(payload);
        }

        return engineRouting.hasModel(requestedModel)
                .flatMap(has -> {
                    if (Boolean.TRUE.equals(has)) {
                        return Mono.just(payload);
                    }
                    return engineRouting.pickFirstAvailableModel()
                            .map(model -> {
                                if (model != null && !model.isBlank()) {
                                    System.out.println("[ChatController] requested model '" + requestedModel + "' not found, switching to '" + model + "'");
                                    Map<String, Object> replaced = new java.util.HashMap<>(payload);
                                    replaced.put("model", model);
                                    return replaced;
                                }
                                return payload;
                            });
                })
                .onErrorResume(e -> {
                    System.err.println("[ChatController] model resolution error: " + e.getMessage());
                    return Mono.just(payload);
                });
    }
}
