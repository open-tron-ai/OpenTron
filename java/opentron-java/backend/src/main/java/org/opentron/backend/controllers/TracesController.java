package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/v1/traces")
public class TracesController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTraces(
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> traces = new ArrayList<>();
        
        for (int i = 0; i < Math.min(limit, 10); i++) {
            List<Map<String, Object>> steps = new ArrayList<>();
            int stepCount = 5 + (int)(Math.random() * 10);
            for (int s = 0; s < stepCount; s++) {
                Map<String, Object> step = new HashMap<>();
                step.put("step_type", s % 3 == 0 ? "retrieve" : s % 2 == 0 ? "generate" : "route");
                step.put("duration_ms", 50 + Math.random() * 200);
                Map<String, Object> data = new HashMap<>();
                data.put("tokens", 50 + (int)(Math.random() * 100));
                data.put("model", "mistral");
                step.put("data", data);
                steps.add(step);
            }
            
            Map<String, Object> trace = new HashMap<>();
            trace.put("id", "trace-" + i);
            trace.put("query", "Sample query " + i);
            trace.put("created_at", Instant.now().minusSeconds(i * 30).toString());
            trace.put("steps", steps);
            trace.put("outcome", i % 3 == 0 ? "error" : "success");
            if (i % 3 == 0) {
                trace.put("error_message", "Connection timeout");
            }
            traces.add(trace);
        }
        
        return ResponseEntity.ok(Map.of("traces", traces));
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<Map<String, Object>> getTrace(@PathVariable String traceId) {
        List<Map<String, Object>> steps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> step = new HashMap<>();
            step.put("step_type", "inference");
            step.put("duration_ms", 50 + Math.random() * 200);
            Map<String, Object> data = new HashMap<>();
            data.put("input", "User query " + i);
            data.put("output", "Model response " + i);
            data.put("tokens", 125);
            data.put("model", "mistral");
            step.put("data", data);
            steps.add(step);
        }
        
        return ResponseEntity.ok(Map.of(
            "id", traceId,
            "query", "Sample query",
            "created_at", Instant.now().toString(),
            "steps", steps,
            "outcome", "success"
        ));
    }
}
