package org.opentron.backend.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.opentron.backend.agents.ScreenshotAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class ScreenshotController {

    @Autowired
    private ScreenshotAnalyzer screenshotAnalyzer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyze a screenshot and suggest improvements
     * POST /v1/analyze-screenshot
     * Body: {
     *   "image_base64": "data:image/png;base64,...",
     *   "prompt": "What improvements would you suggest?",
     *   "context": "React login form"
     * }
     */
    @PostMapping("/analyze-screenshot")
    public ResponseEntity<Map<String, Object>> analyzeScreenshot(
            @RequestBody Map<String, Object> request) {
        try {
            System.out.println("[ScreenshotController] Received analyze-screenshot request");
            
            String imageBase64 = (String) request.get("image_base64");
            String prompt = (String) request.getOrDefault("prompt", "Analyze this screenshot and suggest improvements");
            String context = (String) request.getOrDefault("context", "");

            if (imageBase64 == null || imageBase64.isBlank()) {
                System.err.println("[ScreenshotController] Missing image_base64");
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "error", "image_base64 is required"));
            }

            System.out.println("[ScreenshotController] Calling screenshotAnalyzer.analyzeScreenshot()");
            Map<String, Object> analysis = screenshotAnalyzer.analyzeScreenshot(
                imageBase64, prompt, context);

            System.out.println("[ScreenshotController] Analysis result: " + objectMapper.writeValueAsString(analysis));

            // Handle null response
            if (analysis == null) {
                System.err.println("[ScreenshotController] Analysis returned null");
                return ResponseEntity.status(500)
                    .body(Map.of("status", "error", "error", "Screenshot analysis returned null"));
            }

            // Check if analysis contains error status
            String status = (String) analysis.getOrDefault("status", "unknown");
            System.out.println("[ScreenshotController] Analysis status: " + status);
            
            if ("error".equals(status)) {
                System.err.println("[ScreenshotController] Analysis error: " + analysis.get("error"));
                return ResponseEntity.status(500).body(analysis);
            }

            System.out.println("[ScreenshotController] Returning completed analysis");
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            System.err.println("[ScreenshotController] Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("status", "error", "error", e.getMessage()));
        }
    }
}
