package org.opentron.backend.compose;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ComposeService {

    public List<Map<String, Object>> listCompositions() {
        return List.of(
            Map.of(
                "name", "hello_world",
                "kind", "discrete",
                "model", "gpt-4",
                "agent_type", "direct",
                "tools", List.of("search", "browser"),
                "description", "Example composition placeholder for Java compose migration"
            ),
            Map.of(
                "name", "operator_sample",
                "kind", "operator",
                "model", "gpt-4o",
                "agent_type", "autonomous",
                "tools", List.of("calendar", "email"),
                "description", "Example operator composition placeholder"
            )
        );
    }

    public Map<String, Object> getComposition(String name) {
        if ("hello_world".equals(name)) {
            return Map.ofEntries(
                Map.entry("name", "hello_world"),
                Map.entry("kind", "discrete"),
                Map.entry("model", "gpt-4"),
                Map.entry("quantization", "fp16"),
                Map.entry("provider", "openai"),
                Map.entry("engine_key", "openai-gpt4"),
                Map.entry("agent_type", "direct"),
                Map.entry("temperature", 0.7),
                Map.entry("tools", List.of("search", "browser")),
                Map.entry("description", "Example composition placeholder for Java compose migration"),
                Map.entry("system_prompt", "You are an assistant that answers queries using available tools."),
                Map.entry("eval_benchmarks", List.of("gsm8k", "strategyqa")),
                Map.entry("schedule_type", null),
                Map.entry("schedule_value", null),
                Map.entry("channels", List.of())
            );
        }
        if ("operator_sample".equals(name)) {
            return Map.ofEntries(
                Map.entry("name", "operator_sample"),
                Map.entry("kind", "operator"),
                Map.entry("model", "gpt-4o"),
                Map.entry("quantization", "fp16"),
                Map.entry("provider", "openai"),
                Map.entry("engine_key", "openai-gpt4o"),
                Map.entry("agent_type", "autonomous"),
                Map.entry("temperature", 0.5),
                Map.entry("tools", List.of("calendar", "email")),
                Map.entry("description", "Example operator composition placeholder"),
                Map.entry("system_prompt", "You are an autonomous assistant that manages tasks and notifications."),
                Map.entry("schedule_type", "cron"),
                Map.entry("schedule_value", "0 * * * *"),
                Map.entry("channels", List.of("slack", "email"))
            );
        }
        return null;
    }

    public Map<String, Object> runComposition(ComposeRunRequest request) {
        String runId = String.format("compose-run-%d", Instant.now().toEpochMilli());
        return Map.of(
            "id", runId,
            "name", request.getName(),
            "query", request.getQuery(),
            "status", "started",
            "result", "Placeholder result for composition '" + request.getName() + "'",
            "message", "Composition run initiated successfully"
        );
    }

    public Map<String, Object> benchComposition(ComposeBenchRequest request) {
        return Map.of(
            "name", request.getName(),
            "benchmarks", request.getBenchmarks(),
            "max_samples", request.getMaxSamples(),
            "judge_model", request.getJudgeModel(),
            "verbose", request.isVerbose(),
            "results", List.of(
                Map.of(
                    "benchmark", request.getBenchmarks() == null || request.getBenchmarks().isEmpty() ? "example" : request.getBenchmarks().get(0),
                    "accuracy", 0.88,
                    "correct", 44,
                    "scored_samples", 50,
                    "errors", 0
                )
            )
        );
    }

    public Map<String, Object> deployComposition(ComposeActionRequest request) {
        return Map.of(
            "name", request.getName(),
            "status", "deployed",
            "message", "Operator composition deployed successfully"
        );
    }

    public Map<String, Object> stopComposition(ComposeActionRequest request) {
        return Map.of(
            "name", request.getName(),
            "status", "stopped",
            "message", "Operator composition stopped successfully"
        );
    }

    public Map<String, Object> getStatus() {
        return Map.of(
            "operators", List.of(
                Map.of(
                    "name", "operator_sample",
                    "state", "running",
                    "schedule", "cron 0 * * * *",
                    "last_run", Instant.now().minusSeconds(420).toString()
                )
            )
        );
    }
}
