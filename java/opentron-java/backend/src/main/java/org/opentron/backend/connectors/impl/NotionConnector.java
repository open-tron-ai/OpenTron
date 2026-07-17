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
 * Notion connector — uses the Notion REST API v1 with an integration token (ntn_...).
 * Fetches all pages the token has access to, plus their full block content.
 */
@Component
public class NotionConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(NotionConnector.class);
    private static final String BASE = "https://api.notion.com/v1";
    private static final String VERSION = "2022-06-28";
    private static final int PAGE_SIZE = 100;

    private final WebClient client = WebClient.create(BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "notion"; }
    @Override public String name()      { return "Notion"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Notion integration token is required (ntn_...)";
            return false;
        }
        try {
            String body = client.get().uri("/users/me")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .header("Notion-Version", VERSION)
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("status") && root.path("status").asInt() >= 400) {
                lastError = root.path("message").asText("Auth failed");
                return false;
            }
            logger.info("[Notion] Connected as {}", root.path("name").asText("unknown"));
            return true;
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String token = credentialToken.trim();

        try {
            // Search for all pages (POST /search with empty query returns everything)
            String cursor = null;
            boolean hasMore = true;

            while (hasMore) {
                Map<String, Object> body = new HashMap<>();
                body.put("page_size", PAGE_SIZE);
                body.put("filter", Map.of("value", "page", "property", "object"));
                if (cursor != null) body.put("start_cursor", cursor);
                if (since != null) body.put("sort", Map.of(
                    "direction", "descending", "timestamp", "last_edited_time"));

                String resp = client.post().uri("/search")
                    .header("Authorization", "Bearer " + token)
                    .header("Notion-Version", VERSION)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve().bodyToMono(String.class).block();

                JsonNode root = mapper.readTree(resp);
                hasMore = root.path("has_more").asBoolean(false);
                cursor  = root.path("next_cursor").asText(null);
                if (cursor != null && cursor.equals("null")) cursor = null;

                for (JsonNode page : root.path("results")) {
                    try {
                        String pageId    = page.path("id").asText();
                        String title     = extractPageTitle(page);
                        String editedAt  = page.path("last_edited_time").asText();
                        String pageUrl   = page.path("url").asText();

                        // Skip if not modified since last sync
                        if (since != null && !editedAt.isEmpty()) {
                            if (editedAt.compareTo(since) < 0) continue;
                        }

                        String content = fetchPageContent(token, pageId);
                        if (content.isBlank()) continue;

                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id",      "notion:" + pageId);
                        doc.put("title",   title);
                        doc.put("content", title + "\n\n" + content);
                        doc.put("url",     pageUrl);
                        doc.put("ts",      editedAt.isEmpty() ? java.time.Instant.now().toString() : editedAt);
                        docs.add(doc);
                    } catch (Exception e) {
                        logger.debug("[Notion] Skipping page: {}", e.getMessage());
                    }
                }

                if (cursor == null) hasMore = false;
            }
            logger.info("[Notion] Fetched {} pages", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[Notion] fetchDocuments failed", e);
        }
        return docs;
    }

    private String extractPageTitle(JsonNode page) {
        JsonNode props = page.path("properties");
        // Title property can be called "Name" or "title"
        for (String key : new String[]{"title", "Name", "Title"}) {
            JsonNode prop = props.path(key);
            if (!prop.isMissingNode()) {
                JsonNode titleArr = prop.path("title");
                if (titleArr.isArray() && !titleArr.isEmpty()) {
                    return titleArr.get(0).path("plain_text").asText("Untitled");
                }
            }
        }
        return "Untitled";
    }

    private String fetchPageContent(String token, String pageId) {
        StringBuilder sb = new StringBuilder();
        try {
            String resp = client.get()
                .uri("/blocks/" + pageId + "/children?page_size=100")
                .header("Authorization", "Bearer " + token)
                .header("Notion-Version", VERSION)
                .retrieve().bodyToMono(String.class).block();

            JsonNode root = mapper.readTree(resp);
            for (JsonNode block : root.path("results")) {
                String type = block.path("type").asText();
                JsonNode blockData = block.path(type);
                JsonNode richText = blockData.path("rich_text");
                if (richText.isArray()) {
                    for (JsonNode rt : richText) {
                        sb.append(rt.path("plain_text").asText());
                    }
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            logger.debug("[Notion] Could not fetch blocks for {}: {}", pageId, e.getMessage());
        }
        return sb.toString().trim();
    }
}
