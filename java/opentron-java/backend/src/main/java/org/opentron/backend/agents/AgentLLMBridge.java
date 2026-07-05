package org.opentron.backend.agents;

import org.opentron.backend.util.CloudModelService;
import org.opentron.backend.util.OllamaCliService;
import org.opentron.backend.util.HuggingFaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * AgentLLMBridge - Single clean call to Ollama, no retries, no timeouts.
 * Virtual threads handle the wait; only fails if Ollama is genuinely down.
 */
public class AgentLLMBridge {

    private final OllamaCliService ollamaService;
    private final HuggingFaceService huggingFaceService;
    private final CloudModelService cloudModelService;
    private final String model;
    private final boolean useHF;
    private final Map<String, String> apiKeyOverrides;
    private static final Logger logger = LoggerFactory.getLogger(AgentLLMBridge.class);

    public AgentLLMBridge(OllamaCliService ollamaService, HuggingFaceService huggingFaceService, String model) {
        this(ollamaService, huggingFaceService, null, model, null);
    }

    public AgentLLMBridge(OllamaCliService ollamaService, HuggingFaceService huggingFaceService, CloudModelService cloudModelService,
                          String model, Map<String, String> apiKeyOverrides) {
        this.ollamaService = ollamaService;
        this.huggingFaceService = huggingFaceService;
        this.cloudModelService = cloudModelService;
        this.model = model != null ? model : "mistral";
        this.useHF = System.getenv("HF_MODE") != null &&
                    ("local".equalsIgnoreCase(System.getenv("HF_MODE")) ||
                     "api".equalsIgnoreCase(System.getenv("HF_MODE")));
        this.apiKeyOverrides = apiKeyOverrides == null ? Collections.emptyMap() : apiKeyOverrides;
    }

    /**
     * Query LLM — single attempt, waits indefinitely (virtual thread parks).
     * Only returns error if Ollama is unreachable or returns HTTP error.
     */
    public Map<String, Object> queryLLM(String systemPrompt, String userQuestion, int maxTokens) {
        try {
            logger.info("Querying {}...", model);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userQuestion));

            Map<String, Object> response = invokeModel(messages);

            if (response == null) {
                return errorResponse("LLM returned null response");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return errorResponse("Empty choices in Ollama response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> choice = choices.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = message != null ? (String) message.get("content") : "";

            if (content == null || content.isBlank()) {
                return errorResponse("Empty content from LLM");
            }

            logger.info("{} responded: {} chars", model, content.length());
            return parseRecommendations(content, response);

        } catch (Exception e) {
            logger.error("Error querying LLM", e);
            return errorResponse("LLM unavailable: " + e.getMessage());
        }
    }

    private boolean isCloudModel(String modelName) {
        if (modelName == null || modelName.isBlank()) return false;
        String lower = modelName.toLowerCase();
        return lower.startsWith("gpt-") || lower.startsWith("gpt4") || lower.startsWith("o1-") ||
               lower.startsWith("o3-") || lower.startsWith("o4-") || lower.startsWith("chatgpt-") ||
               lower.startsWith("claude-") || lower.startsWith("gemini-") ||
               lower.startsWith("openrouter/") || lower.startsWith("anthropic/") || lower.startsWith("minimax-");
    }

    private Map<String, Object> invokeModel(List<Map<String, String>> messages) {
        if (isCloudModel(model)) {
            if (cloudModelService == null) {
                throw new IllegalStateException("Cloud model service not configured for model: " + model);
            }
            logger.info("Routing cloud model {} through CloudModelService", model);
            return cloudModelService.callCloudModel(model, messages, apiKeyOverrides).block();
        }

        if (useHF) {
            return huggingFaceService.chatCompletion(model, messages).block();
        }

        return ollamaService.chatCompletion(model, messages).block();
    }

    private Map<String, Object> parseRecommendations(String content, Map<String, Object> response) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("response", content);

        List<String> recommendations = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.matches("^[-•*\\d+.]+\\s+.*")) {
                String rec = trimmed.replaceAll("^[-•*\\d+.]+\\s+", "").trim();
                if (!rec.isEmpty()) recommendations.add(rec);
            }
        }
        if (recommendations.isEmpty()) {
            for (String sentence : content.split("[.!?]+")) {
                String s = sentence.trim();
                if (s.length() > 10) recommendations.add(s);
            }
        }
        result.put("recommendations", recommendations);

        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null) {
            result.put("tokens_used", usage.get("total_tokens"));
        }

        return result;
    }

    private Map<String, Object> errorResponse(String error) {
        return Map.of("status", "error", "error", error, "recommendations", List.of());
    }
}
