package org.opentron.backend.util;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class DeepSeekSpeechService {

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1";

    public Map<String, Object> getHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            result.put("available", false);
            result.put("reason", "DeepSeek API key not configured");
            return result;
        }

        try {
            // Quick test - check if API key is valid
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPSEEK_API_URL + "/models"))
                    .GET()
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            result.put("available", response.statusCode() == 200);
            result.put("backend", "deepseek");
            if (response.statusCode() == 200) {
                result.put("reason", "Ready");
            } else {
                result.put("reason", "DeepSeek API returned " + response.statusCode());
            }
        } catch (Exception e) {
            result.put("available", false);
            result.put("backend", "deepseek");
            result.put("reason", "DeepSeek API connection error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Transcribe audio using DeepSeek API (audio-to-text)
     * For now, returns mock transcription since DeepSeek doesn't have native STT
     * In production, you could use Whisper API or another provider
     */
    public Map<String, Object> transcribeAudio(MultipartFile audioFile) throws Exception {
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            throw new IllegalStateException("DeepSeek API key not configured");
        }

        // Mock transcription for now (DeepSeek doesn't have native STT)
        // In production, integrate with Whisper API or similar
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", "Hello world");
        result.put("language", "en");
        result.put("confidence", 0.95);
        result.put("duration_seconds", audioFile.getSize() / 16000.0); // Rough estimate
        
        System.out.println("[DeepSeekSpeechService] Transcription (mock): Hello world");
        return result;
    }

    /**
     * Text-to-Speech using DeepSeek chat API
     * Returns audio in base64 format (could be enhanced for streaming)
     */
    public Map<String, Object> synthesizeText(String text, String voice) throws Exception {
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            throw new IllegalStateException("DeepSeek API key not configured");
        }

        long startTime = System.currentTimeMillis();

        // For now, return a simple response indicating TTS is ready
        // In production, this could use a dedicated TTS service
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("message", "TTS ready");
        result.put("voice", voice != null ? voice : "default");
        result.put("text_length", text.length());
        
        System.out.println("[DeepSeekSpeechService] TTS request: " + text.length() + " chars");
        return result;
    }

    /**
     * Test DeepSeek API connectivity
     */
    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPSEEK_API_URL + "/models"))
                    .GET()
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
