package org.opentron.backend.controllers;

import org.opentron.backend.agents.Agent;
import org.opentron.backend.agents.AgentCreateRequest;
import org.opentron.backend.agents.AgentService;
import org.opentron.backend.agents.MessageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/v1/agents")
public class AgentsController {

    private final AgentService agentService;

    public AgentsController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Agent>>> listAgents() {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(agentService.listAgents()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Agent>> createAgent(@RequestBody Mono<AgentCreateRequest> body) {
        return body.defaultIfEmpty(new AgentCreateRequest())
                .map(req -> agentService.createAgent(req))
                .map(a -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(a));
    }

    @DeleteMapping(path = "/{agentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> killAgent(@PathVariable String agentId) {
        boolean ok = agentService.killAgent(agentId);
        if (ok) return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Collections.singletonMap("id", agentId)));
        else return Mono.just(ResponseEntity.status(404).body(Collections.singletonMap("error", "not_found")));
    }

    @PostMapping(path = "/{agentId}/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Agent>> messageAgent(@PathVariable String agentId, @RequestBody Mono<MessageRequest> body) {
        return body.flatMap(req -> {
            Agent a = agentService.sendMessage(agentId, req);
            if (a == null) return Mono.just(ResponseEntity.status(404).body(null));
            return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(a));
        });
    }
}

