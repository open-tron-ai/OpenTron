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
 * Slack connector — uses the Slack Web API with a User OAuth token (xoxp-).
 * credentialToken = xoxp-... user token (has access to all channels/DMs the user is in).
 *
 * Fetches conversations list, then history for each channel/DM/group up to `since`.
 */
@Component
public class SlackConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(SlackConnector.class);
    private static final String BASE = "https://slack.com/api";
    private static final int MAX_CHANNELS = 200;
    private static final int MESSAGES_PER_CHANNEL = 100;

    private final WebClient client = WebClient.create(BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "slack"; }
    @Override public String name()      { return "Slack"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Slack user token is required (xoxp-...)";
            return false;
        }
        try {
            String body = client.get()
                .uri("/auth.test")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (!root.path("ok").asBoolean(false)) {
                lastError = "Slack auth failed: " + root.path("error").asText();
                return false;
            }
            logger.info("[Slack] Connected as {}", root.path("user").asText());
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
        String oldest = since != null
            ? String.valueOf(java.time.Instant.parse(since).getEpochSecond()) : "0";

        try {
            // 1. List all conversation types the user has access to
            String chanBody = client.get()
                .uri(u -> u.path("/conversations.list")
                    .queryParam("types", "public_channel,private_channel,mpim,im")
                    .queryParam("limit", MAX_CHANNELS)
                    .queryParam("exclude_archived", true).build())
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(String.class).block();

            JsonNode chanRoot = mapper.readTree(chanBody);
            if (!chanRoot.path("ok").asBoolean(false)) {
                lastError = chanRoot.path("error").asText();
                return docs;
            }

            for (JsonNode ch : chanRoot.path("channels")) {
                String chanId   = ch.path("id").asText();
                String chanName = ch.has("name") ? "#" + ch.path("name").asText()
                                                 : ch.path("user").asText("DM");
                try {
                    String histBody = client.get()
                        .uri(u -> u.path("/conversations.history")
                            .queryParam("channel", chanId)
                            .queryParam("oldest", oldest)
                            .queryParam("limit", MESSAGES_PER_CHANNEL).build())
                        .header("Authorization", "Bearer " + token)
                        .retrieve().bodyToMono(String.class).block();

                    JsonNode hist = mapper.readTree(histBody);
                    if (!hist.path("ok").asBoolean(false)) continue;

                    for (JsonNode msg : hist.path("messages")) {
                        String text = msg.path("text").asText();
                        if (text.isBlank()) continue;
                        String ts = msg.path("ts").asText();
                        String user = msg.path("user").asText("unknown");
                        double tsDouble = Double.parseDouble(ts.isEmpty() ? "0" : ts);
                        String isoTs = java.time.Instant.ofEpochSecond((long) tsDouble).toString();

                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id",      "slack:" + chanId + ":" + ts);
                        doc.put("title",   chanName + " — " + user);
                        doc.put("content", "[" + chanName + "] " + user + ": " + text);
                        doc.put("url",     "");
                        doc.put("ts",      isoTs);
                        docs.add(doc);
                    }
                } catch (Exception e) {
                    logger.debug("[Slack] Skipping channel {}: {}", chanId, e.getMessage());
                }
            }
            logger.info("[Slack] Fetched {} messages", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[Slack] fetchDocuments failed", e);
        }
        return docs;
    }
}
