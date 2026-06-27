package org.opentron.backend.controllers;

import org.opentron.backend.compose.ComposeActionRequest;
import org.opentron.backend.compose.ComposeBenchRequest;
import org.opentron.backend.compose.ComposeRunRequest;
import org.opentron.backend.compose.ComposeService;
import org.springframework.http.MediaType;
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

@RestController
@RequestMapping("/v1/compose")
public class ComposeController {

    private final ComposeService composeService;

    public ComposeController(ComposeService composeService) {
        this.composeService = composeService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Map<String, Object>>>> listCompositions() {
        List<Map<String, Object>> compositions = composeService.listCompositions();
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(compositions));
    }

    @GetMapping(path = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> showComposition(@PathVariable String name) {
        Map<String, Object> composition = composeService.getComposition(name);
        if (composition == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(composition));
    }

    @PostMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> runComposition(@RequestBody Mono<ComposeRunRequest> body) {
        return body.defaultIfEmpty(new ComposeRunRequest())
                .map(composeService::runComposition)
                .map(result -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result));
    }

    @PostMapping(path = "/bench", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> benchComposition(@RequestBody Mono<ComposeBenchRequest> body) {
        return body.defaultIfEmpty(new ComposeBenchRequest())
                .map(composeService::benchComposition)
                .map(result -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result));
    }

    @PostMapping(path = "/deploy", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> deployComposition(@RequestBody Mono<ComposeActionRequest> body) {
        return body.defaultIfEmpty(new ComposeActionRequest())
                .map(composeService::deployComposition)
                .map(result -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result));
    }

    @PostMapping(path = "/stop", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> stopComposition(@RequestBody Mono<ComposeActionRequest> body) {
        return body.defaultIfEmpty(new ComposeActionRequest())
                .map(composeService::stopComposition)
                .map(result -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result));
    }

    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> status() {
        Map<String, Object> status = composeService.getStatus();
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(status));
    }
}
