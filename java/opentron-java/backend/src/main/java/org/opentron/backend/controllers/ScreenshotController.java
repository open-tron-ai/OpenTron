package org.opentron.backend.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.opentron.backend.agents.ScreenshotAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class ScreenshotController {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotController.class);

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
            @RequestBody org.opentron.backend.dto.ScreenshotAnalyzeRequest request) {
        try {
            logger.info("Received analyze-screenshot request");
            
            String imageBase64 = request.getImage_base64();
            String prompt = request.getPrompt() == null ? "Analyze this screenshot and suggest improvements" : request.getPrompt();
            String context = request.getContext() == null ? "" : request.getContext();

            if (imageBase64 == null || imageBase64.isBlank()) {
                logger.warn("Missing image_base64");
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "error", "image_base64 is required"));
            }

            logger.debug("Calling screenshotAnalyzer.analyzeScreenshot()");
            Map<String, Object> analysis = screenshotAnalyzer.analyzeScreenshot(
                imageBase64, prompt, context);

            logger.debug("Analysis result: {}", objectMapper.writeValueAsString(analysis));

            // Handle null response
            if (analysis == null) {
                logger.warn("Analysis returned null");
                return ResponseEntity.status(500)
                    .body(Map.of("status", "error", "error", "Screenshot analysis returned null"));
            }

            // Check if analysis contains error status
            String status = (String) analysis.getOrDefault("status", "unknown");
            logger.debug("Analysis status: {}", status);
            
            if ("error".equals(status)) {
                logger.warn("Analysis error: {}", analysis.get("error"));
                return ResponseEntity.status(500).body(analysis);
            }
            logger.info("Returning completed analysis");
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            logger.error("Exception in analyzeScreenshot", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "error", "error", e.getMessage()));
        }
    }
}
