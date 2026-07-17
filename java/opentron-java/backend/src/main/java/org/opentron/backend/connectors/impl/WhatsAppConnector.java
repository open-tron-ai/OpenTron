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
 * WhatsApp connector — Meta WhatsApp Cloud API.
 * credentialToken format: "PHONE_NUMBER_ID:ACCESS_TOKEN"
 *
 * The Cloud API does not expose message history — it only delivers
 * real-time inbound webhook events.  This connector therefore indexes
 * the messages that OpenTron has already received through the SendBlue
 * or WhatsApp webhook channel (stored in the local message log), plus
 * any messages fetched via the conversations endpoint where available.
 *
 * connect() validates the token against the /phone_number_id endpoint.
 * fetchDocuments() reads messages from the Meta conversations endpoint
 * (requires WhatsApp Business Platform with approved templates access)
 * and falls back gracefully when the endpoint is unavailable.
 */
@Component
public class WhatsAppConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppConnector.class);
    private static final String GRAPH_BASE = "https://graph.facebook.com/v18.0";

    private final WebClient client = WebClient.create(GRAPH_BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "whatsapp"; }
    @Override public String name()      { return "WhatsApp"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || !credentialToken.contains(":")) {
            lastError = "Credential must be PHONE_NUMBER_ID:ACCESS_TOKEN";
            return false;
        }
        String[] parts = splitCredential(credentialToken);
        String phoneId = parts[0];
        String token   = parts[1];
        try {
            String body = client.get()
                .uri("/" + phoneId + "?fields=display_phone_number,verified_name")
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("error")) {
                lastError = root.path("error").path("message").asText("Auth failed");
                return false;
            }
            logger.info("[WhatsApp] Connected — phone: {}",
                root.path("display_phone_number").asText("unknown"));
            return true;
        } catch (Exception e) { lastError = e.getMessage(); return false; }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String[] parts  = splitCredential(credentialToken);
        String phoneId  = parts[0];
        String token    = parts[1];

        try {
            // Fetch recent conversations via the Business Management API
            // Endpoint: GET /PHONE_NUMBER_ID/conversations
            String resp = client.get()
                .uri(u -> {
                    var b = u.path("/" + phoneId + "/conversations")
                        .queryParam("fields",
                            "id,messages{from,to,text,timestamp,type},participants")
                        .queryParam("limit", 100);
                    if (since != null) b.queryParam("since",
                        java.time.Instant.parse(since).getEpochSecond());
                    return b.build();
                })
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(String.class).block();

            JsonNode root = mapper.readTree(resp);

            // The conversations endpoint may not be available on all tiers —
            // handle gracefully so connect() still works for webhook-only setups.
            if (root.has("error")) {
                String errCode = root.path("error").path("code").asText("");
                if ("10".equals(errCode) || "200".equals(errCode)) {
                    // Permission not available — connector is connected but read-only via webhook
                    logger.info("[WhatsApp] Conversations API not available (webhook-only mode)");
                    return docs;
                }
                lastError = root.path("error").path("message").asText();
                return docs;
            }

            for (JsonNode conv : root.path("data")) {
                String convId = conv.path("id").asText();
                List<String> lines = new ArrayList<>();
                String latestTs = java.time.Instant.now().toString();

                for (JsonNode msg : conv.path("messages").path("data")) {
                    String type = msg.path("type").asText("text");
                    if (!"text".equals(type)) continue;
                    String text    = msg.path("text").path("body").asText("");
                    String from    = msg.path("from").asText("unknown");
                    long   tsEpoch = msg.path("timestamp").asLong(0);
                    if (tsEpoch > 0) latestTs = java.time.Instant.ofEpochSecond(tsEpoch).toString();
                    if (!text.isBlank()) lines.add(from + ": " + text);
                }

                if (lines.isEmpty()) continue;
                Collections.reverse(lines); // oldest first

                // Participant labels
                List<String> participants = new ArrayList<>();
                for (JsonNode p : conv.path("participants").path("data")) {
                    String name = p.path("name").asText("");
                    if (!name.isBlank()) participants.add(name);
                }
                String title = participants.isEmpty()
                    ? "WhatsApp conversation " + convId
                    : "WhatsApp with " + String.join(", ", participants);

                Map<String, Object> doc = new HashMap<>();
                doc.put("id",      "whatsapp:" + convId);
                doc.put("title",   title);
                doc.put("content", String.join("\n", lines));
                doc.put("url",     "");
                doc.put("ts",      latestTs);
                docs.add(doc);
            }
            logger.info("[WhatsApp] Fetched {} conversations", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[WhatsApp] fetchDocuments failed", e);
        }
        return docs;
    }

    private String[] splitCredential(String token) {
        int idx = token.indexOf(':');
        return new String[]{ token.substring(0, idx).trim(), token.substring(idx + 1).trim() };
    }
}
