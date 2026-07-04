package org.opentron.backend.agents;

import org.opentron.backend.util.OllamaCliService;
import org.opentron.backend.util.HuggingFaceService;

import java.util.*;

/**
 * AgentLLMBridge - Single clean call to Ollama, no retries, no timeouts.
 * Virtual threads handle the wait; only fails if Ollama is genuinely down.
 */
public class AgentLLMBridge {

    private final OllamaCliService ollamaService;
    private final HuggingFaceService huggingFaceService;
    private final String model;
    private final boolean useHF;

    public AgentLLMBridge(OllamaCliService ollamaService, HuggingFaceService huggingFaceService, String model) {
        this.ollamaService = ollamaService;
        this.huggingFaceService = huggingFaceService;
        this.model = model != null ? model : "mistral";
        this.useHF = System.getenv("HF_MODE") != null &&
                    ("local".equalsIgnoreCase(System.getenv("HF_MODE")) ||
                     "api".equalsIgnoreCase(System.getenv("HF_MODE")));
    }

    /**
     * Query LLM — single attempt, waits indefinitely (virtual thread parks).
     * Only returns error if Ollama is unreachable or returns HTTP error.
     */
    public Map<String, Object> queryLLM(String systemPrompt, String userQuestion, int maxTokens) {
        try {
            System.out.println("[AgentLLMBridge] Querying " + model + "...");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userQuestion));

            // Block without timeout — virtual thread parks until Ollama responds
            Map<String, Object> response = useHF
                ? huggingFaceService.chatCompletion(model, messages).block()
                : ollamaService.chatCompletion(model, messages).block();

            if (response == null) {
                return errorResponse("Ollama returned null response");
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
                return errorResponse("Empty content from Ollama");
            }

            System.out.println("[AgentLLMBridge] " + model + " responded: " + content.length() + " chars");
            return parseRecommendations(content, response);

        } catch (Exception e) {
            System.err.println("[AgentLLMBridge] Error: " + e.getMessage());
            return errorResponse("Ollama unavailable: " + e.getMessage());
        }
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
