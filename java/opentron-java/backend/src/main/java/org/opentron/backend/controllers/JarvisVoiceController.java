package org.opentron.backend.controllers;

import org.opentron.backend.util.JarvisVoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Jarvis Voice API Controller
 * Provides Jarvis-style TTS and voice-related endpoints
 */
@RestController
@RequestMapping("/v1/jarvis")
public class JarvisVoiceController {
    private static final Logger logger = LoggerFactory.getLogger(JarvisVoiceController.class);
    
    @Autowired
    private JarvisVoiceService jarvisVoiceService;
    
    /**
     * Health check for Jarvis voice system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "operational",
            "voice", "jarvis",
            "type", "local_tts",
            "backend", "ollama",
            "features", Arrays.asList("text-to-speech", "voice-synthesis", "professional-tone")
        ));
    }
    
    /**
     * Synthesize text with Jarvis voice
     * Request body: { "text": "Your message here" }
     */
    @PostMapping("/speak")
    public ResponseEntity<JarvisVoiceService.SynthesisResponse> speak(
            @RequestBody org.opentron.backend.dto.JarvisSpeakRequest request) {
        
        String text = request.getText();
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                JarvisVoiceService.SynthesisResponse.error("Text is required")
            );
        }
        
        logger.info("[JarvisAPI] Speaking: {}", text.substring(0, Math.min(50, text.length())));
        
        JarvisVoiceService.SynthesisResponse response = jarvisVoiceService.synthesizeWithJarvis(text);
        
        if ("success".equals(response.status)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get Jarvis voice characteristics
     */
    @GetMapping("/voice-profile")
    public ResponseEntity<Map<String, Object>> voiceProfile() {
        return ResponseEntity.ok(Map.of(
            "name", "Jarvis",
            "description", "Professional AI assistant voice - deep, formal, and composed",
            "characteristics", Map.of(
                "pitch", "Low (0.8) - Deep voice",
                "speed", "Moderate (0.9) - Clear articulation",
                "formality", "High - Professional tone",
                "accent", "British - Formal delivery",
                "emotion", "Neutral - Calm and composed",
                "brightness", "Warm (0.6) - Pleasant but professional"
            ),
            "example_usage", "Perfect for AI assistant, narrator, or formal announcements"
        ));
    }
    
    /**
     * Batch synthesis - synthesize multiple texts
     */
    @PostMapping("/batch-speak")
    public ResponseEntity<List<JarvisVoiceService.SynthesisResponse>> batchSpeak(
            @RequestBody List<String> texts) {
        
        List<JarvisVoiceService.SynthesisResponse> responses = new ArrayList<>();
        
        for (String text : texts) {
            JarvisVoiceService.SynthesisResponse response = jarvisVoiceService.synthesizeWithJarvis(text);
            responses.add(response);
        }
        
        return ResponseEntity.ok(responses);
    }
}
