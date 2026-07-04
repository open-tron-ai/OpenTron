package org.opentron.backend.services;

import org.springframework.stereotype.Service;
import org.opentron.backend.util.OllamaCliService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ModelSelectorService - Intelligently selects the best model for each task
 * 
 * Maps agent specialization → optimal model from available pool
 * Maximizes speed and quality by choosing purpose-built models
 */
@Service
public class ModelSelectorService {
    
    private final OllamaCliService ollamaService;
    private final Map<String, String> modelCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    public ModelSelectorService(OllamaCliService ollamaService) {
        this.ollamaService = ollamaService;
        loadAvailableModels();
    }

    /**
     * Select the best available model for a given agent type
     */
    public String selectBestModel(String agentType) {
        refreshModelCacheIfNeeded();
        
        System.out.println("[ModelSelector] Selecting model for: " + agentType);
        
        switch (agentType.toLowerCase()) {
            case "backend":
                return selectBackendModel();
            case "frontend":
                return selectFrontendModel();
            case "qa":
                return selectQAModel();
            case "devops":
                return selectDevOpsModel();
            default:
                return selectGeneralModel();
        }
    }

    /**
     * Backend specialist: optimize for code, caching, database queries, API design
     * Prefers: neural-chat (code-optimized) > qwen3.50.8b > mistral
     */
    private String selectBackendModel() {
        if (hasModel("neural-chat")) {
            System.out.println("[ModelSelector] Backend → neural-chat (code-optimized)");
            return "neural-chat";
        }
        if (hasModel("qwen3.50.8b")) {
            System.out.println("[ModelSelector] Backend → qwen3.50.8b (fallback)");
            return "qwen3.50.8b";
        }
        System.out.println("[ModelSelector] Backend → mistral (last resort)");
        return "mistral";
    }

    /**
     * Frontend specialist: optimize for React, TypeScript, components, state management
     * Prefers: neural-chat (code-optimized) > qwen3.5.9b > mistral
     */
    private String selectFrontendModel() {
        if (hasModel("neural-chat")) {
            System.out.println("[ModelSelector] Frontend → neural-chat (React expert)");
            return "neural-chat";
        }
        if (hasModel("qwen3.5.9b")) {
            System.out.println("[ModelSelector] Frontend → qwen3.5.9b (fast, capable)");
            return "qwen3.5.9b";
        }
        System.out.println("[ModelSelector] Frontend → mistral (last resort)");
        return "mistral";
    }

    /**
     * QA specialist: optimize for testing, debugging, code review
     * Prefers: neural-chat (code review expert) > qwen3.5.2b (very fast) > mistral
     */
    private String selectQAModel() {
        if (hasModel("neural-chat")) {
            System.out.println("[ModelSelector] QA → neural-chat (code review)");
            return "neural-chat";
        }
        if (hasModel("qwen3.5.2b")) {
            System.out.println("[ModelSelector] QA → qwen3.5.2b (very fast)");
            return "qwen3.5.2b";
        }
        System.out.println("[ModelSelector] QA → mistral (last resort)");
        return "mistral";
    }

    /**
     * DevOps specialist: optimize for monitoring, metrics, infrastructure
     * Prefers: qwen3.50.8b (larger, complex configs) > neural-chat > mistral
     */
    private String selectDevOpsModel() {
        if (hasModel("qwen3.50.8b")) {
            System.out.println("[ModelSelector] DevOps → qwen3.50.8b (infrastructure)");
            return "qwen3.50.8b";
        }
        if (hasModel("neural-chat")) {
            System.out.println("[ModelSelector] DevOps → neural-chat (fallback)");
            return "neural-chat";
        }
        System.out.println("[ModelSelector] DevOps → mistral (last resort)");
        return "mistral";
    }

    /**
     * General purpose: select fastest available
     * Prefers: neural-chat > qwen3.5.9b > qwen3.5.2b > mistral
     */
    private String selectGeneralModel() {
        if (hasModel("neural-chat")) return "neural-chat";
        if (hasModel("qwen3.5.9b")) return "qwen3.5.9b";
        if (hasModel("qwen3.5.2b")) return "qwen3.5.2b";
        return "mistral";
    }

    /**
     * Check if a model is available
     */
    private boolean hasModel(String modelName) {
        return modelCache.containsKey(modelName);
    }

    /**
     * Load available models from Ollama
     */
    private void loadAvailableModels() {
        System.out.println("[ModelSelector] Loading available models...");
        try {
            // This will be populated by a background call to OllamaCliService.listModels()
            // For now, assume models are available
            modelCache.put("neural-chat", "available");
            modelCache.put("qwen3.50.8b", "available");
            modelCache.put("qwen3.5.9b", "available");
            modelCache.put("qwen3.5.2b", "available");
            modelCache.put("mistral", "available");
            modelCache.put("llama2", "available");
            modelCache.put("llava", "available");
            lastCacheUpdate = System.currentTimeMillis();
            System.out.println("[ModelSelector] Loaded " + modelCache.size() + " models");
        } catch (Exception e) {
            System.err.println("[ModelSelector] Error loading models: " + e.getMessage());
            // Fallback to mistral
            modelCache.put("mistral", "available");
        }
    }

    /**
     * Refresh model cache if TTL expired
     */
    private void refreshModelCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_TTL_MS) {
            System.out.println("[ModelSelector] Cache TTL expired, refreshing...");
            loadAvailableModels();
        }
    }

    /**
     * Get model information for debugging
     */
    public Map<String, Object> getModelSelectorStatus() {
        refreshModelCacheIfNeeded();
        return Map.of(
            "available_models", modelCache.keySet(),
            "model_count", modelCache.size(),
            "last_cache_update", lastCacheUpdate,
            "cache_ttl_ms", CACHE_TTL_MS,
            "backend_model", selectBackendModel(),
            "frontend_model", selectFrontendModel(),
            "qa_model", selectQAModel(),
            "devops_model", selectDevOpsModel()
        );
    }
}
