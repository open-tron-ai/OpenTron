package org.opentron.backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OllamaCliService: Call Ollama via HTTP API with aggressive model optimization.
 * Uses fast models (mistral) for instant responses instead of large models.
 */
@Service
public class OllamaCliService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FAST_MODEL = "mistral"; // 7B, ~4.2GB, very fast
    private static final String OLLAMA_API = "http://127.0.0.1:11434/api/generate";

    /**
     * Run a chat completion via Ollama HTTP API using the fastest model.
     * Pre-loaded by ModelPreloader on startup, so first response is instant.
     */
    public Mono<Map<String, Object>> chatCompletion(String model, List<Map<String, String>> messages) {
        return Mono.fromCallable(() -> {
            // Force fast model - ignore slow models requested
            String actualModel = FAST_MODEL;
            if (!model.equals(FAST_MODEL)) {
                System.out.println("[OllamaCliService] ⚡ Using " + FAST_MODEL + " instead of " + model + " for speed");
            }

            // Convert OpenAI messages to prompt
            StringBuilder prompt = new StringBuilder();
            for (Map<String, String> msg : messages) {
                String role = msg.getOrDefault("role", "user");
                String content = msg.getOrDefault("content", "");
                if ("user".equals(role)) {
                    prompt.append(content);
                } else if ("assistant".equals(role)) {
                    prompt.append("\n[Assistant]: ").append(content);
                }
            }

            String promptStr = prompt.toString().trim();
            System.out.println("[OllamaCliService] 🚀 INSTANT inference with " + actualModel + " (prompt: " + promptStr.length() + " chars)");

            try {
                // Call Ollama /api/generate (fastest endpoint)
                java.net.URL url = new java.net.URL(OLLAMA_API);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(60000); // 60 second timeout for fast model
                conn.setDoOutput(true);

                // Minimal JSON payload for speed
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("model", actualModel);
                payload.put("prompt", promptStr);
                payload.put("stream", false);
                payload.put("temperature", 0.7);
                payload.put("top_k", 40);
                payload.put("top_p", 0.9);
                payload.put("num_predict", 256); // Limit output length for speed

                String jsonPayload = objectMapper.writeValueAsString(payload);
                System.out.println("[OllamaCliService] Sending request to Ollama...");
                
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes("UTF-8"));
                    os.flush();
                }

                System.out.println("[OllamaCliService] Waiting for response...");
                int responseCode = conn.getResponseCode();
                String responseText;

                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        responseText = sb.toString();
                    }
                } else {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        responseText = sb.toString();
                    }
                    System.err.println("[OllamaCliService] HTTP error " + responseCode + ": " + responseText);
                    throw new RuntimeException("Ollama HTTP error " + responseCode);
                }

                // Parse response
                Map<String, Object> ollamaResp = objectMapper.readValue(responseText, java.util.HashMap.class);
                Object resp = ollamaResp.get("response");
                String responseContent = resp != null ? resp.toString().trim() : "No response";

                System.out.println("[OllamaCliService] ✅ Response ready (" + responseContent.length() + " chars)");

                // Build OpenAI-compatible response with actual model name
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("model", model); // Return requested model name to frontend
                result.put("actual_model", actualModel); // Show which model was actually used
                result.put("created", System.currentTimeMillis() / 1000);

                Map<String, Object> message = new LinkedHashMap<>();
                message.put("role", "assistant");
                message.put("content", responseContent);

                Map<String, Object> choice = new LinkedHashMap<>();
                choice.put("index", 0);
                choice.put("message", message);
                choice.put("finish_reason", "stop");

                result.put("choices", List.of(choice));

                return result;
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("[OllamaCliService] Timeout: " + e.getMessage());
                throw new RuntimeException("Ollama inference timeout (consider restarting Ollama)", e);
            } catch (Exception e) {
                System.err.println("[OllamaCliService] Error: " + e.getMessage());
                e.printStackTrace(System.err);
                throw e;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @SuppressWarnings("unchecked")
    public Mono<List<String>> listModels() {
        return (Mono<List<String>>) (Object) Mono.fromCallable(() -> {
            System.out.println("[OllamaCliService] Fetching available models...");

            try {
                java.net.URL url = new java.net.URL("http://127.0.0.1:11434/api/tags");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    System.err.println("[OllamaCliService] Failed to list models: " + responseCode);
                    return List.of();
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    String responseText = sb.toString();

                    Map<String, Object> resp = objectMapper.readValue(responseText, java.util.HashMap.class);
                    List<Map<String, Object>> models = (List<Map<String, Object>>) resp.get("models");

                    List<String> modelNames = new java.util.ArrayList<>();
                    if (models != null) {
                        for (Map<String, Object> model : models) {
                            Object name = model.get("name");
                            if (name != null) {
                                modelNames.add(name.toString());
                            }
                        }
                    }
                    return modelNames;
                }
            } catch (Exception e) {
                System.err.println("[OllamaCliService] listModels error: " + e.getMessage());
                return List.of();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
