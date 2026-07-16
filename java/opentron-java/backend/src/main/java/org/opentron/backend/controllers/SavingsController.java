package org.opentron.backend.controllers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.opentron.backend.telemetry.TelemetryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/savings")
public class SavingsController {

    private final TelemetryService telemetryService;

    public SavingsController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> savings() {
        Map<String, Object> payload = new LinkedHashMap<>();
        
        // Get real inference activity from telemetry
        long totalCalls = telemetryService.getTotalRequests();
        long totalTokens = telemetryService.getTotalTokens();
        long totalEnergyJ = telemetryService.getTotalEnergyJ();
        
        // Estimate token split (rough: 60% prompt, 40% completion)
        long totalPromptTokens = Math.round(totalTokens * 0.6);
        long totalCompletionTokens = Math.round(totalTokens * 0.4);
        
        // Local cost estimation: Qwen 32B on consumer GPU ~1e-6 USD/token
        double localCost = totalTokens * 1e-6;
        
        payload.put("total_calls", totalCalls);
        payload.put("total_prompt_tokens", totalPromptTokens);
        payload.put("total_completion_tokens", totalCompletionTokens);
        payload.put("total_tokens", totalTokens);
        payload.put("total_energy_j", totalEnergyJ);
        payload.put("local_cost", localCost);
        
        // Per-provider comparison data (dynamic based on total tokens)
        var providers = new ArrayList<Map<String, Object>>();
        
        // Claude Opus 4.6: input $5/1M, output $25/1M
        double claudeCost = (totalPromptTokens * 5.0 + totalCompletionTokens * 25.0) / 1_000_000;
        double claudeEnergy = totalTokens > 0 ? (claudeCost / 0.0005) * 0.00027 : 0.0; // rough conversion
        Map<String, Object> provider1 = new LinkedHashMap<>();
        provider1.put("provider", "claude-opus-4.6");
        provider1.put("total_cost", claudeCost);
        provider1.put("energy_wh", claudeEnergy);
        provider1.put("flops", Math.round(totalTokens * 1e10));
        providers.add(provider1);
        
        // GPT-5.3: input $2/1M, output $10/1M
        double gptCost = (totalPromptTokens * 2.0 + totalCompletionTokens * 10.0) / 1_000_000;
        double gptEnergy = totalTokens > 0 ? (gptCost / 0.0005) * 0.00027 : 0.0;
        Map<String, Object> provider2 = new LinkedHashMap<>();
        provider2.put("provider", "gpt-5.3");
        provider2.put("total_cost", gptCost);
        provider2.put("energy_wh", gptEnergy);
        provider2.put("flops", Math.round(totalTokens * 0.6e10));
        providers.add(provider2);
        
        // Gemini 3.1 Pro: input $2/1M, output $12/1M
        double geminiCost = (totalPromptTokens * 2.0 + totalCompletionTokens * 12.0) / 1_000_000;
        double geminiEnergy = totalTokens > 0 ? (geminiCost / 0.0005) * 0.00027 : 0.0;
        Map<String, Object> provider3 = new LinkedHashMap<>();
        provider3.put("provider", "gemini-3.1-pro");
        provider3.put("total_cost", geminiCost);
        provider3.put("energy_wh", geminiEnergy);
        provider3.put("flops", Math.round(totalTokens * 0.55e10));
        providers.add(provider3);
        
        payload.put("per_provider", providers);
        payload.put("token_counting_version", 1);

        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload));
    }
}
