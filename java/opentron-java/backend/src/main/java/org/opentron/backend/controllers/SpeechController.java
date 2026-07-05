package org.opentron.backend.controllers;

import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.opentron.backend.util.ElevenLabsSpeechService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/speech")
public class SpeechController {

    @Autowired
    private ElevenLabsSpeechService elevenLabsSpeechService;

    @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getSpeechHealth() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = elevenLabsSpeechService.getHealth();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(health);
        });
    }

    @PostMapping(path = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> transcribeAudio(
            @RequestParam("file") MultipartFile audioFile) {
        
        if (audioFile.isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("detail", "No audio file provided");
            return Mono.just(ResponseEntity.badRequest().body(error));
        }

        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> result = elevenLabsSpeechService.transcribeAudio(audioFile);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
            } catch (IllegalStateException e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("detail", e.getMessage());
                return ResponseEntity.status(503).body(error);
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("detail", e.getMessage() != null ? e.getMessage() : "Transcription failed");
                return ResponseEntity.status(500).body(error);
            }
        });
    }

    @PostMapping(path = "/synthesize", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> synthesizeText(
            @RequestBody org.opentron.backend.dto.SynthesizeTextRequest request) {
        
        String text = request.getText() == null ? "" : request.getText();
        String voice = request.getVoice() == null ? "" : request.getVoice();
        
        if (text.isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("detail", "No text provided");
            return Mono.just(ResponseEntity.badRequest().body(error));
        }

        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> result = elevenLabsSpeechService.synthesizeText(text, voice);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
            } catch (IllegalStateException e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("detail", e.getMessage());
                return ResponseEntity.status(503).body(error);
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("detail", e.getMessage() != null ? e.getMessage() : "Synthesis failed");
                return ResponseEntity.status(500).body(error);
            }
        });
    }

    @GetMapping(path = "/voices", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> listVoices() {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> voices = elevenLabsSpeechService.listVoices();
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(voices);
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("detail", e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
        });
    }
}
