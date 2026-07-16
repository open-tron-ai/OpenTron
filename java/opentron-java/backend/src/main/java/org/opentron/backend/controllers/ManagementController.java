package org.opentron.backend.controllers;

import org.springframework.http.MediaType;
import org.opentron.backend.dto.ToolCredentialsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import org.opentron.backend.approvals.ApprovalsService;
import org.opentron.backend.agents.AgentService;
import org.opentron.backend.util.EngineRouting;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/v1")
public class ManagementController {

    private final ApprovalsService approvalsService;
    private final AgentService agentService;
    private final EngineRouting engineRouting;

    @Autowired
    public ManagementController(ApprovalsService approvalsService, AgentService agentService, EngineRouting engineRouting) {
        this.approvalsService = approvalsService;
        this.agentService = agentService;
        this.engineRouting = engineRouting;
    }

    @GetMapping(path = "/recommended-model", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> recommendedModel() {
        return engineRouting.pickFirstAvailableModel()
                .map(model -> {
                    if (model == null || model.isBlank()) {
                        return ResponseEntity.status(404)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Map.<String, Object>of("error", "model not available"));
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.<String, Object>of("model", model));
                })
                .onErrorReturn(ResponseEntity.status(502)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.<String, Object>of("error", "model selection failed")));
    }

    @GetMapping(path = "/approvals/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<String>>> approvalsPending() {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(approvalsService.listPending()));
    }

    @GetMapping(path = "/managed-agents", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Object>>> managedAgents() {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(List.copyOf(agentService.listAgents())));
    }

    @GetMapping(path = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> serverInfo() {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of(
                "version", "0.1.0",
                "service", "opentron-java-backend",
                "status", "ok"
        )));
    }

    @GetMapping(path = "/analytics/identity", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> analyticsIdentity() {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of(
                "enabled", true,
                "anon_id", "opentron-anon-" + System.getenv().getOrDefault("HOSTNAME", "local"),
                "host", "https://analytics.example.com",
                "key", "ph_public_XXXXXXXXXXXXXX"
        )));
    }

    @PostMapping(path = "/tools/{toolName}/credentials", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> saveToolCredentials(@PathVariable String toolName, @RequestBody ToolCredentialsRequest payload) {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of(
                "status", "success",
                "tool", toolName
        )));
    }
}
