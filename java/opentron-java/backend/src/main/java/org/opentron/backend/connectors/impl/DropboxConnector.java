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
 * Dropbox connector — Dropbox API v2.
 * credentialToken = Dropbox OAuth2 access token (sl.u...).
 * Lists all files recursively, downloads text files up to 10 MB.
 */
@Component
public class DropboxConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(DropboxConnector.class);
    private static final String CONTENT_BASE = "https://content.dropboxapi.com";
    private static final String API_BASE     = "https://api.dropboxapi.com";
    private static final long   MAX_BYTES    = 10 * 1024 * 1024;

    private final WebClient apiClient     = WebClient.create(API_BASE);
    private final WebClient contentClient = WebClient.create(CONTENT_BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "dropbox"; }
    @Override public String name()      { return "Dropbox"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Dropbox access token required"; return false;
        }
        try {
            String body = apiClient.post().uri("/2/users/get_current_account")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .header("Content-Type", "application/json")
                .bodyValue("null")
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("error_summary")) { lastError = root.path("error_summary").asText(); return false; }
            logger.info("[Dropbox] Connected as {}", root.path("email").asText());
            return true;
        } catch (Exception e) { lastError = e.getMessage(); return false; }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String token = credentialToken.trim();

        try {
            // List folder recursively from root
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("path", "");
            reqBody.put("recursive", true);
            reqBody.put("include_deleted", false);
            reqBody.put("include_media_info", false);
            reqBody.put("limit", 2000);

            boolean hasMore = true;
            String cursor  = null;

            while (hasMore) {
                String resp;
                if (cursor == null) {
                    resp = apiClient.post().uri("/2/files/list_folder")
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .bodyValue(reqBody)
                        .retrieve().bodyToMono(String.class).block();
                } else {
                    resp = apiClient.post().uri("/2/files/list_folder/continue")
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .bodyValue(Map.of("cursor", cursor))
                        .retrieve().bodyToMono(String.class).block();
                }

                JsonNode root = mapper.readTree(resp);
                hasMore = root.path("has_more").asBoolean(false);
                cursor  = root.path("cursor").asText(null);

                for (JsonNode entry : root.path("entries")) {
                    if (!"file".equals(entry.path(".tag").asText())) continue;

                    String path     = entry.path("path_display").asText();
                    String name     = entry.path("name").asText();
                    long   size     = entry.path("size").asLong(0);
                    String modified = entry.path("server_modified").asText();

                    // Skip if not modified since last sync
                    if (since != null && !modified.isEmpty() && modified.compareTo(since) < 0) continue;
                    // Skip large and binary files
                    if (size > MAX_BYTES) continue;
                    if (!isTextFile(name)) continue;

                    try {
                        String content = contentClient.post()
                            .uri("/2/files/download")
                            .header("Authorization", "Bearer " + token)
                            .header("Dropbox-API-Arg", "{\"path\":\"" + path + "\"}")
                            .retrieve().bodyToMono(String.class).block();

                        if (content == null || content.isBlank()) continue;

                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id",      "dropbox:" + path);
                        doc.put("title",   name);
                        doc.put("content", name + "\n\n" + content);
                        doc.put("url",     "https://www.dropbox.com/home" + path);
                        doc.put("ts",      modified.isEmpty() ? java.time.Instant.now().toString() : modified);
                        docs.add(doc);
                    } catch (Exception e) {
                        logger.debug("[Dropbox] Could not download {}: {}", path, e.getMessage());
                    }
                }
            }
            logger.info("[Dropbox] Fetched {} files", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[Dropbox] fetchDocuments failed", e);
        }
        return docs;
    }

    private boolean isTextFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")
            || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".html")
            || lower.endsWith(".log")  || lower.endsWith(".rst") || lower.endsWith(".yaml")
            || lower.endsWith(".yml");
    }
}
