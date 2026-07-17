package org.opentron.backend.connectors.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentron.backend.connectors.DataConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Granola connector — uses the Granola REST API.
 * credentialToken = Granola API key (grn_...).
 * Fetches meeting notes from /v1/notes.
 */
@Component
public class GranolaConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(GranolaConnector.class);
    private static final String BASE = "https://api.granola.so";

    private final WebClient client = WebClient.create(BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "granola"; }
    @Override public String name()      { return "Granola"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Granola API key required (grn_...)"; return false;
        }
        try {
            String body = client.get().uri("/v1/me")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("error")) { lastError = root.path("error").asText(); return false; }
            logger.info("[Granola] Connected as {}", root.path("email").asText("unknown"));
            return true;
        } catch (Exception e) { lastError = e.getMessage(); return false; }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String token = credentialToken.trim();

        try {
            String cursor = null;
            boolean hasMore = true;

            while (hasMore) {
                String finalCursor = cursor;
                String resp = client.get()
                    .uri(u -> {
                        var b = u.path("/v1/notes").queryParam("limit", 50);
                        if (finalCursor != null) b.queryParam("cursor", finalCursor);
                        if (since != null)       b.queryParam("after", since);
                        return b.build();
                    })
                    .header("Authorization", "Bearer " + token)
                    .retrieve().bodyToMono(String.class).block();

                JsonNode root = mapper.readTree(resp);
                if (root.has("error")) { lastError = root.path("error").asText(); break; }

                cursor  = root.path("next_cursor").asText(null);
                hasMore = cursor != null && !cursor.isBlank();

                for (JsonNode note : root.path("notes")) {
                    try {
                        String noteId  = note.path("id").asText();
                        String title   = note.path("title").asText("Meeting Note");
                        String content = note.path("content").asText(
                                         note.path("transcript").asText(""));
                        String ts      = note.path("created_at").asText(
                                         java.time.Instant.now().toString());
                        String url     = note.path("url").asText("");

                        if (content.isBlank()) continue;

                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id",      "granola:" + noteId);
                        doc.put("title",   title);
                        doc.put("content", title + "\n\n" + content);
                        doc.put("url",     url);
                        doc.put("ts",      ts);
                        docs.add(doc);
                    } catch (Exception e) {
                        logger.debug("[Granola] Skipping note: {}", e.getMessage());
                    }
                }
            }
            logger.info("[Granola] Fetched {} notes", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[Granola] fetchDocuments failed", e);
        }
        return docs;
    }
}
