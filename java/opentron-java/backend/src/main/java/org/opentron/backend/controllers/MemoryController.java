package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/v1/memory")
public class MemoryController {

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMemoryStats() {
        return ResponseEntity.ok(Map.of(
            "entries", 245,
            "backend", "sqlite",
            "size_mb", 12.4,
            "last_indexed", System.currentTimeMillis()
        ));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getMemoryConfig() {
        return ResponseEntity.ok(Map.of(
            "backend", "sqlite",
            "available", true,
            "context_from_memory", true,
            "context_top_k", 5,
            "context_min_score", 0.5,
            "context_max_tokens", 2000
        ));
    }

    @PostMapping("/store")
    public ResponseEntity<Map<String, Object>> storeMemory(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of(
            "status", "stored",
            "id", UUID.randomUUID().toString()
        ));
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMemory(@RequestBody Map<String, Object> payload) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        String query = (String) payload.get("query");
        int topK = payload.containsKey("top_k") ? ((Number) payload.get("top_k")).intValue() : 5;
        
        for (int i = 0; i < topK; i++) {
            Map<String, Object> result = new HashMap<>();
            result.put("content", "Memory entry " + i + " related to: " + query);
            result.put("score", 0.95 - (i * 0.05));
            result.put("metadata", Map.of("source", "document-" + i, "date", System.currentTimeMillis()));
            results.add(result);
        }
        
        return ResponseEntity.ok(Map.of("results", results));
    }

    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> indexPath(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of(
            "chunks_indexed", 42,
            "note", "Successfully indexed path"
        ));
    }
}
