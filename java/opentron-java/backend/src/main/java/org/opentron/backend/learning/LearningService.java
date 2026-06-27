package org.opentron.backend.learning;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LearningService {
    public Map<String, Object> getStats() {
        return Map.of(
                "skill_discovery", Map.of(
                        "enabled", false,
                        "available", List.of(
                                Map.of("name", "model_routing", "description", "Chooses the best model for each task"),
                                Map.of("name", "agent_advisor", "description", "Provides advisor guidance for agent decisions"),
                                Map.of("name", "icl_updater", "description", "Updates in-context learning examples for agents")
                        )
                ),
                "model_routing", Map.of(
                        "candidates", List.of("gpt-4.1", "gpt-4o", "gpt-3.5-turbo"),
                        "preferred", "gpt-4.1"
                ),
                "active_learners", 0,
                "last_updated", "2026-06-24T00:00:00Z"
        );
    }

    public Map<String, Object> getPolicy() {
        return Map.of(
                "enabled", false,
                "routing", Map.of(
                        "policy", "heuristic",
                        "min_samples", 0,
                        "strategy", "static",
                        "fallback", "model_routing"
                ),
                "intelligence", Map.of(
                        "policy", "none",
                        "model", "none",
                        "temperature", 0.7,
                        "fallback_model", "gpt-3.5-turbo"
                ),
                "agent", Map.of(
                        "policy", "none",
                        "advisor_enabled", false,
                        "icl_enabled", false
                ),
                "metrics", Map.of(
                        "reward_weights", Map.of(
                                "accuracy", 1.0,
                                "efficiency", 0.5,
                                "coverage", 0.25
                        ),
                        "evaluation_interval", "24h"
                )
        );
    }
}
