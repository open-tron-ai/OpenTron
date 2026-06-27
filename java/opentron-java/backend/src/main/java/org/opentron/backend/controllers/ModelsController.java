package org.opentron.backend.controllers;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import org.opentron.backend.util.EngineRouting;
import org.opentron.backend.util.ResilienceUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/models")
public class ModelsController {

    private final WebClient webClient;
    private final EngineRouting engineRouting;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelsController(WebClient webClient, EngineRouting engineRouting) {
        this.webClient = webClient;
        this.engineRouting = engineRouting;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> listModels() {
        String targetPath = engineRouting.translateRequestPath("/v1/models");
        return ResilienceUtil.withResilienceNonStreaming(
            webClient.get()
                    .uri(targetPath)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        try {
                            // Parse the Ollama response: {"models": [...]}
                            Map<String, Object> ollamaResp = objectMapper.readValue(response, Map.class);
                            List<Map<String, Object>> ollamaModels = (List<Map<String, Object>>) ollamaResp.get("models");
                            
                            // Transform each model to OpenAI format
                            List<Map<String, Object>> transformed = new ArrayList<>();
                            if (ollamaModels != null) {
                                for (Map<String, Object> ollamaModel : ollamaModels) {
                                    Map<String, Object> modelInfo = new LinkedHashMap<>();
                                    
                                    // Extract the model name (e.g., "mistral:latest" → "mistral")
                                    String modelName = (String) ollamaModel.get("name");
                                    if (modelName == null) {
                                        modelName = (String) ollamaModel.get("model");
                                    }
                                    
                                    // OpenAI format expects:
                                    modelInfo.put("id", modelName != null ? modelName : "unknown");
                                    modelInfo.put("object", "model");
                                    modelInfo.put("created", System.currentTimeMillis() / 1000);
                                    modelInfo.put("owned_by", "ollama");
                                    
                                    transformed.add(modelInfo);
                                }
                            }
                            
                            // Build response: {"data": [...]}
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("data", transformed);
                            
                            String jsonResponse = objectMapper.writeValueAsString(result);
                            System.out.println("[ModelsController] Transformed " + transformed.size() + " models");
                            
                            return ResponseEntity.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jsonResponse);
                        } catch (Exception e) {
                            System.err.println("[ModelsController] Error transforming response: " + e.getMessage());
                            e.printStackTrace(System.err);
                            // Return empty data on error
                            try {
                                Map<String, Object> fallback = new LinkedHashMap<>();
                                fallback.put("data", new ArrayList<>());
                                return ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(objectMapper.writeValueAsString(fallback));
                            } catch (Exception ex) {
                                return ResponseEntity.status(502)
                                        .body("{\"data\":[],\"error\":\"models-service-unavailable\"}");
                            }
                        }
                    })
                    .onErrorReturn(ResponseEntity.status(502)
                            .body("{\"data\":[],\"error\":\"models-service-unavailable\"}"))
        );
    }
}
