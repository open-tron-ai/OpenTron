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
 * CloudModelService: Handle cloud API models (OpenAI, Anthropic, Google, etc.)
 * Routes requests to appropriate cloud APIs based on model prefix
 */
@Service
public class CloudModelService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Determine which cloud provider to use based on model name
     */
    private String getCloudProvider(String model) {
        if (model == null) return null;
        String lower = model.toLowerCase();
        
        if (lower.startsWith("gpt-") || lower.startsWith("gpt4") || lower.startsWith("o1-") || 
            lower.startsWith("o3-") || lower.startsWith("o4-") || lower.startsWith("chatgpt-")) {
            return "openai";
        } else if (lower.startsWith("claude-")) {
            return "anthropic";
        } else if (lower.startsWith("gemini-")) {
            return "google";
        } else if (lower.startsWith("openrouter/")) {
            return "openrouter";
        } else if (lower.startsWith("minimax-")) {
            return "minimax";
        }
        return null;
    }

    /**
     * Get API key for the cloud provider from environment or system property
     */
    private String getApiKey(String provider) {
        switch (provider) {
            case "openai":
                return System.getenv("OPENAI_API_KEY");
            case "anthropic":
                return System.getenv("ANTHROPIC_API_KEY");
            case "google":
                return System.getenv("GOOGLE_API_KEY") != null ? System.getenv("GOOGLE_API_KEY") : System.getenv("GEMINI_API_KEY");
            case "openrouter":
                return System.getenv("OPENROUTER_API_KEY");
            case "minimax":
                return System.getenv("MINIMAX_API_KEY");
            default:
                return null;
        }
    }

    /**
     * Convert provider name to frontend config key
     */
    private String getFrontendKeyName(String provider) {
        switch (provider) {
            case "openai":
                return "openai";
            case "anthropic":
                return "anthropic";
            case "google":
                return "google";
            case "openrouter":
                return "openrouter";
            case "minimax":
                return "minimax";
            default:
                return provider;
        }
    }

    /**
     * Get API key for the cloud provider, with override parameter
     * Prioritizes frontend-provided API keys over environment variables
     */
    private String getApiKey(String provider, Map<String, String> apiKeyOverrides) {
        String key = null;
        
        // First, try to get from frontend overrides (highest priority)
        if (apiKeyOverrides != null) {
            switch (provider) {
                case "openai":
                    key = apiKeyOverrides.get("openai");
                    break;
                case "anthropic":
                    key = apiKeyOverrides.get("anthropic");
                    break;
                case "google":
                    key = apiKeyOverrides.get("google");
                    break;
                case "openrouter":
                    key = apiKeyOverrides.get("openrouter");
                    break;
                case "minimax":
                    key = apiKeyOverrides.get("minimax");
                    break;
            }
            
            if (key != null && !key.isBlank()) {
                System.out.println("[CloudModelService] 🔑 Using frontend-provided API key for " + provider);
                return key;
            }
        }

        // Fall back to environment variables if frontend key not provided
        key = getApiKey(provider);
        if (key != null && !key.isBlank()) {
            System.out.println("[CloudModelService] 🔑 Using environment variable API key for " + provider);
            return key;
        }
        
        return null;
    }

    /**
     * Call OpenAI API
     */
    private Map<String, Object> callOpenAI(String model, String apiKey, List<Map<String, String>> messages) throws Exception {
        System.out.println("[CloudModelService] 🔗 Calling OpenAI: " + model);
        
        java.net.URL url = new java.net.URL("https://api.openai.com/v1/chat/completions");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 2000);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes("UTF-8"));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                System.err.println("[CloudModelService] ❌ OpenAI error " + responseCode + ": " + sb);
            }
            throw new RuntimeException("OpenAI API error: " + responseCode);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(sb.toString(), Map.class);
            
            // Extract content and usage
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) throw new RuntimeException("No response from OpenAI");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> choice = choices.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
            String content = messageObj != null ? (String) messageObj.get("content") : "";
            
            // Build OpenAI-compatible response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("model", model);
            result.put("created", System.currentTimeMillis() / 1000);
            
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", content);
            
            Map<String, Object> choiceResult = new LinkedHashMap<>();
            choiceResult.put("index", 0);
            choiceResult.put("message", message);
            choiceResult.put("finish_reason", choice.get("finish_reason"));
            
            result.put("choices", List.of(choiceResult));
            result.put("usage", response.get("usage"));
            
            System.out.println("[CloudModelService] ✅ OpenAI response: " + content.length() + " chars");
            return result;
        }
    }

    /**
     * Call Anthropic (Claude) API
     */
    private Map<String, Object> callAnthropic(String model, String apiKey, List<Map<String, String>> messages) throws Exception {
        System.out.println("[CloudModelService] 🔗 Calling Anthropic (Claude): " + model);
        
        java.net.URL url = new java.net.URL("https://api.anthropic.com/v1/messages");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 2000);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes("UTF-8"));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                System.err.println("[CloudModelService] ❌ Anthropic error " + responseCode + ": " + sb);
            }
            throw new RuntimeException("Anthropic API error: " + responseCode);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(sb.toString(), Map.class);
            
            // Extract content
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content_blocks = (List<Map<String, Object>>) response.get("content");
            String content = "";
            if (content_blocks != null && !content_blocks.isEmpty()) {
                content = (String) content_blocks.get(0).get("text");
            }
            
            // Build OpenAI-compatible response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("model", model);
            result.put("created", System.currentTimeMillis() / 1000);
            
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", content);
            
            Map<String, Object> choice = new LinkedHashMap<>();
            choice.put("index", 0);
            choice.put("message", message);
            choice.put("finish_reason", "stop");
            
            result.put("choices", List.of(choice));
            
            // Estimate usage
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("prompt_tokens", ((Number) response.get("usage.input_tokens")).intValue());
            usage.put("completion_tokens", ((Number) response.get("usage.output_tokens")).intValue());
            result.put("usage", usage);
            
            System.out.println("[CloudModelService] ✅ Anthropic response: " + content.length() + " chars");
            return result;
        }
    }

    /**
     * Call Google Gemini API
     */
    private Map<String, Object> callGoogle(String model, String apiKey, List<Map<String, String>> messages) throws Exception {
        System.out.println("[CloudModelService] 🔗 Calling Google Gemini: " + model);
        
        // Google API expects the full model name (e.g., gemini-2.5-pro)
        // Do NOT strip the gemini- prefix
        java.net.URL url = new java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        // Convert OpenAI format to Gemini format
        java.util.List<Map<String, Object>> contents = new java.util.ArrayList<>();
        for (Map<String, String> msg : messages) {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("role", msg.getOrDefault("role", "user").equals("user") ? "user" : "model");
            
            Map<String, Object> part = new LinkedHashMap<>();
            part.put("text", msg.get("content"));
            
            content.put("parts", List.of(part));
            contents.add(content);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contents", contents);
        
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 2000);
        payload.put("generationConfig", generationConfig);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes("UTF-8"));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                System.err.println("[CloudModelService] ❌ Google error " + responseCode + ": " + sb);
            }
            throw new RuntimeException("Google Gemini API error: " + responseCode);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(sb.toString(), Map.class);
            
            // Extract content
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            String content = "";
            if (candidates != null && !candidates.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> candidate = candidates.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> contentObj = (Map<String, Object>) candidate.get("content");
                if (contentObj != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        content = (String) parts.get(0).get("text");
                    }
                }
            }
            
            // Build OpenAI-compatible response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("model", model);
            result.put("created", System.currentTimeMillis() / 1000);
            
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", content);
            
            Map<String, Object> choice = new LinkedHashMap<>();
            choice.put("index", 0);
            choice.put("message", message);
            choice.put("finish_reason", "stop");
            
            result.put("choices", List.of(choice));
            
            // Extract usage metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> usageMetadata = (Map<String, Object>) response.get("usageMetadata");
            Map<String, Object> usage = new LinkedHashMap<>();
            if (usageMetadata != null) {
                Object promptTokens = usageMetadata.get("promptTokenCount");
                Object completionTokens = usageMetadata.get("candidatesTokenCount");
                if (promptTokens != null) usage.put("prompt_tokens", ((Number) promptTokens).intValue());
                if (completionTokens != null) usage.put("completion_tokens", ((Number) completionTokens).intValue());
            }
            result.put("usage", usage);
            
            System.out.println("[CloudModelService] ✅ Google Gemini response: " + content.length() + " chars");
            return result;
        }
    }

    /**
     * Call cloud model API based on provider with API key overrides from request
     */
    public Mono<Map<String, Object>> callCloudModel(String model, List<Map<String, String>> messages, Map<String, String> apiKeyOverrides) {
        return Mono.fromCallable(() -> {
            String provider = getCloudProvider(model);
            if (provider == null) {
                throw new RuntimeException("Unknown cloud model: " + model);
            }
            
            String apiKey = getApiKey(provider, apiKeyOverrides);
            if (apiKey == null || apiKey.isBlank()) {
                String frontendKey = getFrontendKeyName(provider);
                throw new RuntimeException("API key not configured for " + provider + ". " + 
                    "Please set the API key in the Frontend (Settings > API Keys > " + frontendKey + ") " +
                    "or pass it via X-API-Keys header, " +
                    "or set the " + getEnvVarForProvider(provider) + " environment variable.");
            }
            
            try {
                switch (provider) {
                    case "openai":
                        return callOpenAI(model, apiKey, messages);
                    case "anthropic":
                        return callAnthropic(model, apiKey, messages);
                    case "google":
                        return callGoogle(model, apiKey, messages);
                    case "openrouter":
                        // OpenRouter uses OpenAI-compatible API at api.openrouter.ai
                        return callOpenRouter(model, apiKey, messages);
                    default:
                        throw new RuntimeException("Unsupported provider: " + provider);
                }
            } catch (Exception e) {
                System.err.println("[CloudModelService] ❌ Error calling " + provider + ": " + e.getMessage());
                throw new RuntimeException("Cloud API error: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Call OpenRouter API (OpenAI-compatible)
     */
    private Map<String, Object> callOpenRouter(String model, String apiKey, List<Map<String, String>> messages) throws Exception {
        System.out.println("[CloudModelService] 🔗 Calling OpenRouter: " + model);
        if (apiKey != null && apiKey.length() > 5) {
            System.out.println("[CloudModelService] 🔑 OpenRouter key prefix: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        }
        
        java.net.URL url = new java.net.URL("https://openrouter.ai/api/v1/chat/completions");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        // OpenRouter requires these headers
        conn.setRequestProperty("HTTP-Referer", "https://opentron.local");
        conn.setRequestProperty("X-Title", "OpenTron");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 2000);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes("UTF-8"));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            StringBuilder errorDetails = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) errorDetails.append(line);
            }
            System.err.println("[CloudModelService] ❌ OpenRouter error " + responseCode + ": " + errorDetails);
            if (responseCode == 401) {
                System.err.println("[CloudModelService] ⚠️  401 Unauthorized - Check your OpenRouter API key:");
                System.err.println("[CloudModelService] ⚠️  1. Visit https://openrouter.ai/keys");
                System.err.println("[CloudModelService] ⚠️  2. Create or copy your API key");
                System.err.println("[CloudModelService] ⚠️  3. Key should start with 'sk-or-'");
                System.err.println("[CloudModelService] ⚠️  4. Save it in Settings > API Keys > OpenRouter");
            }
            throw new RuntimeException("OpenRouter API error: " + responseCode + " - " + errorDetails);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(sb.toString(), Map.class);
            
            // Extract content and usage
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) throw new RuntimeException("No response from OpenRouter");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> choice = choices.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
            String content = messageObj != null ? (String) messageObj.get("content") : "";
            
            // Build OpenAI-compatible response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("model", model);
            result.put("created", System.currentTimeMillis() / 1000);
            
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", content);
            
            Map<String, Object> choiceResult = new LinkedHashMap<>();
            choiceResult.put("index", 0);
            choiceResult.put("message", message);
            choiceResult.put("finish_reason", choice.get("finish_reason"));
            
            result.put("choices", List.of(choiceResult));
            result.put("usage", response.get("usage"));
            
            System.out.println("[CloudModelService] ✅ OpenRouter response: " + content.length() + " chars");
            return result;
        }
    }

    private String getEnvVarForProvider(String provider) {
        switch (provider) {
            case "openai":
                return "OPENAI_API_KEY";
            case "anthropic":
                return "ANTHROPIC_API_KEY";
            case "google":
                return "GOOGLE_API_KEY or GEMINI_API_KEY";
            case "openrouter":
                return "OPENROUTER_API_KEY";
            case "minimax":
                return "MINIMAX_API_KEY";
            default:
                return "API_KEY";
        }
    }
}
