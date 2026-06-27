package org.opentron.backend.controllers;

import org.opentron.backend.tools.ToolsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/tools")
public class ToolsController {

    private final ToolsService toolsService;

    public ToolsController(ToolsService toolsService) {
        this.toolsService = toolsService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Map<String, Object>>>> listTools() {
        List<Map<String, Object>> tools = toolsService.listTools();
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(tools));
    }

    @GetMapping(path = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getTool(@PathVariable String name) {
        Map<String, Object> tool = toolsService.getTool(name);
        if (tool == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(tool));
    }
}
