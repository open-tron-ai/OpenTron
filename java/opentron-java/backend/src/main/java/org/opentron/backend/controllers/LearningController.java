package org.opentron.backend.controllers;

import org.opentron.backend.learning.LearningService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v1/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping(path = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getLearningStats() {
        Map<String, Object> stats = learningService.getStats();
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(stats));
    }

    @GetMapping(path = "/policy", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getLearningPolicy() {
        Map<String, Object> policy = learningService.getPolicy();
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(policy));
    }
}
