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
 * Google Drive connector — uses the Drive REST API v3.
 * credentialToken = Google OAuth2 access token.
 * Exports Docs/Sheets/Slides as plain text; downloads other files up to 10 MB.
 */
@Component
public class GDriveConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(GDriveConnector.class);
    private static final String BASE = "https://www.googleapis.com";
    private static final int PAGE_SIZE = 100;
    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final WebClient client = WebClient.create(BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "gdrive"; }
    @Override public String name()      { return "Google Drive"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Google OAuth access token required";
            return false;
        }
        try {
            String body = client.get()
                .uri("/drive/v3/about?fields=user")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("error")) { lastError = root.path("error").path("message").asText(); return false; }
            logger.info("[GDrive] Connected as {}", root.path("user").path("emailAddress").asText());
            return true;
        } catch (Exception e) { lastError = e.getMessage(); return false; }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String token = credentialToken.trim();
        String pageToken = null;

        try {
            String query = "trashed=false";
            if (since != null) query += " and modifiedTime > '" + since + "'";
            final String finalQuery = query;

            do {
                String finalPageToken = pageToken;
                String resp = client.get()
                    .uri(u -> {
                        var b = u.path("/drive/v3/files")
                            .queryParam("q", finalQuery)
                            .queryParam("pageSize", PAGE_SIZE)
                            .queryParam("fields", "nextPageToken,files(id,name,mimeType,modifiedTime,webViewLink,size)");
                        if (finalPageToken != null) b.queryParam("pageToken", finalPageToken);
                        return b.build();
                    })
                    .header("Authorization", "Bearer " + token)
                    .retrieve().bodyToMono(String.class).block();

                JsonNode root = mapper.readTree(resp);
                if (root.has("error")) { lastError = root.path("error").path("message").asText(); break; }
                pageToken = root.path("nextPageToken").asText(null);

                for (JsonNode file : root.path("files")) {
                    try {
                        String fileId   = file.path("id").asText();
                        String name     = file.path("name").asText();
                        String mime     = file.path("mimeType").asText();
                        String modified = file.path("modifiedTime").asText();
                        String url      = file.path("webViewLink").asText();

                        String content = exportOrDownload(token, fileId, mime, file.path("size").asLong(0));
                        if (content == null || content.isBlank()) continue;

                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id",      "gdrive:" + fileId);
                        doc.put("title",   name);
                        doc.put("content", name + "\n\n" + content);
                        doc.put("url",     url);
                        doc.put("ts",      modified.isEmpty() ? java.time.Instant.now().toString() : modified);
                        docs.add(doc);
                    } catch (Exception e) {
                        logger.debug("[GDrive] Skipping file: {}", e.getMessage());
                    }
                }
            } while (pageToken != null && !pageToken.isBlank());

            logger.info("[GDrive] Fetched {} files", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[GDrive] fetchDocuments failed", e);
        }
        return docs;
    }

    private String exportOrDownload(String token, String fileId, String mime, long size) {
        try {
            // Google Workspace types — export as plain text
            if (mime.contains("google-apps.document"))       return exportFile(token, fileId, "text/plain");
            if (mime.contains("google-apps.spreadsheet"))    return exportFile(token, fileId, "text/csv");
            if (mime.contains("google-apps.presentation"))   return exportFile(token, fileId, "text/plain");

            // Binary / large files — skip
            if (size > MAX_BYTES || mime.startsWith("image/") ||
                mime.startsWith("video/") || mime.startsWith("audio/")) return null;

            // Plain text files — download directly
            if (mime.startsWith("text/") || mime.contains("json") || mime.contains("xml")) {
                return client.get()
                    .uri("/drive/v3/files/" + fileId + "?alt=media")
                    .header("Authorization", "Bearer " + token)
                    .retrieve().bodyToMono(String.class).block();
            }
            return null;
        } catch (Exception e) {
            logger.debug("[GDrive] export/download failed for {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    private String exportFile(String token, String fileId, String exportMime) {
        return client.get()
            .uri("/drive/v3/files/" + fileId + "/export?mimeType=" + exportMime)
            .header("Authorization", "Bearer " + token)
            .retrieve().bodyToMono(String.class).block();
    }
}
