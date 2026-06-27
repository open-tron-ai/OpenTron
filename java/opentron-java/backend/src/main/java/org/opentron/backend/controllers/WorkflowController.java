package org.opentron.backend.controllers;

import org.opentron.backend.workflow.WorkflowRunRequest;
import org.opentron.backend.workflow.WorkflowService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Map<String, Object>>>> listWorkflows() {
        List<Map<String, Object>> workflows = workflowService.listWorkflows();
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(workflows));
    }

    @PostMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> runWorkflow(@RequestBody Mono<WorkflowRunRequest> body) {
        return body.defaultIfEmpty(new WorkflowRunRequest())
                .map(workflowService::runWorkflow)
                .map(result -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result));
    }

    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getWorkflowStatus() {
        Map<String, Object> status = new HashMap<>();
        List<Map<String, Object>> running = new ArrayList<>();
        
        // Placeholder: would fetch actual running workflows from WorkflowService
        // For now return empty list to indicate no workflows running
        status.put("running", running);
        status.put("total_running", 0);
        
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(status));
    }
}
