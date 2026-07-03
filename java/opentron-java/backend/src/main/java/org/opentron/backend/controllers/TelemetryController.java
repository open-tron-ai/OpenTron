package org.opentron.backend.controllers;

import org.opentron.backend.telemetry.TelemetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/v1/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/energy")
    public ResponseEntity<Map<String, Object>> getEnergy() {
        List<Map<String, Object>> samples = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Map<String, Object> sample = new HashMap<>();
            sample.put("timestamp", Instant.now().minusSeconds(i * 60).toString());
            sample.put("power_w", 10 + Math.random() * 20);
            sample.put("energy_j", 300 + Math.random() * 200);
            samples.add(sample);
        }
        
        return ResponseEntity.ok(Map.of(
            "total_energy_j", telemetryService.getTotalEnergyJ(),
            "energy_per_token_j", 0.5,
            "avg_power_w", 15.0,
            "samples", samples
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTelemetryStats() {
        return ResponseEntity.ok(Map.of(
            "total_requests", telemetryService.getTotalRequests(),
            "total_tokens", telemetryService.getTotalTokens()
        ));
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> trackTelemetry(@RequestBody Map<String, Object> data) {
        telemetryService.recordRequest();
        Object tokens = data.get("tokens");
        if (tokens instanceof Number) {
            telemetryService.addTokens(((Number) tokens).longValue());
        }
        return ResponseEntity.ok(Map.of("status", "tracked"));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetTelemetry() {
        telemetryService.reset();
        return ResponseEntity.ok(Map.of("status", "reset"));
    }
}
