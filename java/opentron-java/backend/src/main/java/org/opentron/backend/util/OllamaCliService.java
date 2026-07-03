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
 * OllamaCliService: Call Ollama via HTTP API for local models only.
 * Cloud models (gemini, gpt, claude, etc.) are handled by their respective services.
 */
@Service
public class OllamaCliService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String OLLAMA_API = "http://127.0.0.1:11434/api/generate";
    private static final String OLLAMA_TAGS = "http://127.0.0.1:11434/api/tags";

    /**
     * Check if a model is a cloud model (not local Ollama)
     */
    private boolean isCloudModel(String model) {
        if (model == null || model.isBlank()) return false;
        String lower = model.toLowerCase();
        
        // Cloud model prefixes that should NOT use Ollama
        return lower.startsWith("gpt-") ||
               lower.startsWith("gpt4") ||
               lower.startsWith("claude-") ||
               lower.startsWith("gemini-") ||
               lower.startsWith("openrouter/") ||
               lower.startsWith("anthropic/");
    }

    /**
     * Call Ollama with the requested local model
     */
    private Map<String, Object> callOllama(String requestedModel, String promptStr) throws Exception {
        System.out.println("[OllamaCliService] 📤 Calling local Ollama with model: " + requestedModel);
        
        java.net.URL url = new java.net.URL(OLLAMA_API);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(45000); // 45 second timeout for inference
        conn.setDoOutput(true);

        // Prepare request payload - use the requested model
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", requestedModel);
        payload.put("prompt", promptStr);
        payload.put("stream", false);
        payload.put("temperature", 0.7);
        payload.put("top_k", 40);
        payload.put("top_p", 0.9);
        payload.put("num_predict", 256);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        System.out.println("[OllamaCliService] 📨 Sending to Ollama...");
        
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes("UTF-8"));
            os.flush();
        }

        System.out.println("[OllamaCliService] ⏳ Waiting for " + requestedModel + " response...");
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
            System.err.println("[OllamaCliService] ❌ HTTP error " + responseCode);
            throw new RuntimeException("Ollama HTTP error " + responseCode);
        }

        // Parse response
        Map<String, Object> ollamaResp = objectMapper.readValue(responseText, java.util.HashMap.class);
        Object resp = ollamaResp.get("response");
        String responseContent = resp != null ? resp.toString().trim() : "No response";

        // Extract token counts
        long promptTokens = 0;
        long completionTokens = 0;
        if (ollamaResp.containsKey("prompt_eval_count")) {
            promptTokens = ((Number) ollamaResp.get("prompt_eval_count")).longValue();
        }
        if (ollamaResp.containsKey("eval_count")) {
            completionTokens = ((Number) ollamaResp.get("eval_count")).longValue();
        }
        long totalTokens = promptTokens + completionTokens;

        System.out.println("[OllamaCliService] ✅ Got response from " + requestedModel + ": " + responseContent.length() + " chars, tokens: " + totalTokens);

        // Build OpenAI-compatible response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", requestedModel);
        result.put("actual_model", requestedModel);
        result.put("created", System.currentTimeMillis() / 1000);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", responseContent);

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", "stop");

        result.put("choices", List.of(choice));

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("prompt_tokens", promptTokens);
        usage.put("completion_tokens", completionTokens);
        usage.put("total_tokens", totalTokens);
        result.put("usage", usage);

        return result;
    }

    /**
     * Run a chat completion via Ollama HTTP API using the requested local model.
     * Cloud models are rejected - they need their own API clients.
     */
    public Mono<Map<String, Object>> chatCompletion(String model, List<Map<String, String>> messages) {
        return Mono.fromCallable(() -> {
            // Check if this is a cloud model
            if (isCloudModel(model)) {
                System.err.println("[OllamaCliService] ⛔ Cloud model detected: " + model);
                String errorMsg = "Cloud model '" + model + "' is not supported via Ollama. " +
                    "Please use the native API client or configure the appropriate service. " +
                    "Available local models: llama2, mistral, neural-chat, qwen, etc.";
                System.err.println("[OllamaCliService] Error: " + errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // Use the requested local model - no fallback logic
            String requestedModel = model != null && !model.isBlank() ? model : "llama2";
            System.out.println("[OllamaCliService] 🎯 Using requested local model: " + requestedModel);

            // Convert messages to prompt
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
            System.out.println("[OllamaCliService] 💬 Prompt: " + promptStr.length() + " chars");

            // Call Ollama with the requested model - no fallback
            try {
                return callOllama(requestedModel, promptStr);
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("[OllamaCliService] ⏱️ TIMEOUT with " + requestedModel);
                throw new RuntimeException("Model " + requestedModel + " timeout - model may be busy or unavailable", e);
            } catch (Exception e) {
                System.err.println("[OllamaCliService] ❌ Error with " + requestedModel + ": " + e.getMessage());
                throw new RuntimeException("Model " + requestedModel + " error: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * List all available local models from Ollama
     */
    @SuppressWarnings("unchecked")
    public Mono<List<String>> listModels() {
        return (Mono<List<String>>) (Object) Mono.fromCallable(() -> {
            System.out.println("[OllamaCliService] 📋 Fetching local model list from Ollama...");

            try {
                java.net.URL url = new java.net.URL(OLLAMA_TAGS);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    System.err.println("[OllamaCliService] ❌ Failed to list models: " + responseCode);
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
                        for (Map<String, Object> m : models) {
                            Object name = m.get("name");
                            if (name != null) {
                                modelNames.add(name.toString());
                            }
                        }
                    }
                    System.out.println("[OllamaCliService] ✅ Found " + modelNames.size() + " local models");
                    return modelNames;
                }
            } catch (Exception e) {
                System.err.println("[OllamaCliService] ❌ Error fetching models: " + e.getMessage());
                return List.of();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
