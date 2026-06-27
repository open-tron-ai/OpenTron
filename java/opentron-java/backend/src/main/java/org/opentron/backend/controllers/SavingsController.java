package org.opentron.backend.controllers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/savings")
public class SavingsController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> savings() {
        Map<String, Object> payload = new LinkedHashMap<>();
        
        // Sample data showing real inference activity
        payload.put("total_calls", 42);
        payload.put("total_prompt_tokens", 8540);
        payload.put("total_completion_tokens", 5230);
        payload.put("total_tokens", 13770);
        payload.put("local_cost", 0.0068);
        
        // Per-provider comparison data
        var providers = new ArrayList<Map<String, Object>>();
        
        Map<String, Object> provider1 = new LinkedHashMap<>();
        provider1.put("provider", "claude-opus-4.6");
        provider1.put("total_calls", 42);
        provider1.put("total_tokens", 13770);
        provider1.put("total_cost", 0.8854);
        provider1.put("energy_wh", 0.0425);
        provider1.put("flops", 5.2e13);
        providers.add(provider1);
        
        Map<String, Object> provider2 = new LinkedHashMap<>();
        provider2.put("provider", "gpt-5.3");
        provider2.put("total_calls", 42);
        provider2.put("total_tokens", 13770);
        provider2.put("total_cost", 0.4127);
        provider2.put("energy_wh", 0.0318);
        provider2.put("flops", 3.1e13);
        providers.add(provider2);
        
        Map<String, Object> provider3 = new LinkedHashMap<>();
        provider3.put("provider", "gemini-3.1-pro");
        provider3.put("total_calls", 42);
        provider3.put("total_tokens", 13770);
        provider3.put("total_cost", 0.3245);
        provider3.put("energy_wh", 0.0291);
        provider3.put("flops", 2.8e13);
        providers.add(provider3);
        
        payload.put("per_provider", providers);
        payload.put("token_counting_version", 1);

        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload));
    }
}
