package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/v1/managed-agents")
public class ManagedAgentsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listManagedAgents() {
        List<Map<String, Object>> agents = new ArrayList<>();
        
        Map<String, Object> agent = new HashMap<>();
        agent.put("id", "agent-default-1");
        agent.put("name", "My Assistant");
        agent.put("agent_type", "personal_deep_research");
        agent.put("status", "idle");
        agent.put("config", new HashMap<>());
        agent.put("created_at", System.currentTimeMillis() / 1000);
        agent.put("updated_at", System.currentTimeMillis() / 1000);
        agents.add(agent);
        
        return ResponseEntity.ok(Map.of("agents", agents));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createManagedAgent(@RequestBody Map<String, Object> payload) {
        Map<String, Object> agent = new HashMap<>();
        agent.put("id", "agent-" + UUID.randomUUID().toString().substring(0, 8));
        agent.put("name", payload.getOrDefault("name", "New Agent"));
        agent.put("agent_type", payload.getOrDefault("agent_type", payload.getOrDefault("template_id", "default")));
        agent.put("status", "idle");
        agent.put("config", payload.getOrDefault("config", new HashMap<>()));
        agent.put("created_at", System.currentTimeMillis() / 1000);
        agent.put("updated_at", System.currentTimeMillis() / 1000);
        
        return ResponseEntity.ok(agent);
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> getManagedAgent(@PathVariable String agentId) {
        Map<String, Object> agent = new HashMap<>();
        agent.put("id", agentId);
        agent.put("name", "My Assistant");
        agent.put("agent_type", "personal_deep_research");
        agent.put("status", "idle");
        agent.put("config", new HashMap<>());
        agent.put("created_at", System.currentTimeMillis() / 1000);
        agent.put("updated_at", System.currentTimeMillis() / 1000);
        
        return ResponseEntity.ok(agent);
    }
}
