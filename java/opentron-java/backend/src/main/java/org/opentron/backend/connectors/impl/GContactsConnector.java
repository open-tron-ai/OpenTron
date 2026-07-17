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
 * Google Contacts connector — People API v1.
 * credentialToken = Google OAuth2 access token (people.readonly scope).
 */
@Component
public class GContactsConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(GContactsConnector.class);
    private static final String BASE = "https://people.googleapis.com";

    private final WebClient client = WebClient.create(BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "gcontacts"; }
    @Override public String name()      { return "Google Contacts"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Google OAuth access token required"; return false;
        }
        try {
            String body = client.get()
                .uri("/v1/people/me?personFields=names")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("error")) { lastError = root.path("error").path("message").asText(); return false; }
            logger.info("[GContacts] Connected");
            return true;
        } catch (Exception e) { lastError = e.getMessage(); return false; }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String token = credentialToken.trim();
        String pageToken = null;

        final String FIELDS = "names,emailAddresses,phoneNumbers,organizations,biographies,urls,addresses";

        try {
            do {
                String finalPt = pageToken;
                String resp = client.get()
                    .uri(u -> {
                        var b = u.path("/v1/people/me/connections")
                            .queryParam("personFields", FIELDS)
                            .queryParam("pageSize", 200)
                            .queryParam("sortOrder", "LAST_MODIFIED_DESCENDING");
                        if (finalPt != null) b.queryParam("pageToken", finalPt);
                        return b.build();
                    })
                    .header("Authorization", "Bearer " + token)
                    .retrieve().bodyToMono(String.class).block();

                JsonNode root = mapper.readTree(resp);
                if (root.has("error")) { lastError = root.path("error").path("message").asText(); break; }
                pageToken = root.path("nextPageToken").asText(null);

                for (JsonNode person : root.path("connections")) {
                    try {
                        String resourceName = person.path("resourceName").asText();
                        String name  = firstValue(person, "names", "displayName", "Unknown");
                        String email = firstValue(person, "emailAddresses", "value", "");
                        String phone = firstValue(person, "phoneNumbers", "value", "");
                        String org   = firstValue(person, "organizations", "name", "");
                        String bio   = firstValue(person, "biographies", "value", "");

                        String content = "Name: " + name
                            + (email.isBlank() ? "" : "\nEmail: " + email)
                            + (phone.isBlank() ? "" : "\nPhone: " + phone)
                            + (org.isBlank()   ? "" : "\nOrganization: " + org)
                            + (bio.isBlank()   ? "" : "\nNotes: " + bio);

                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id",      "gcontacts:" + resourceName);
                        doc.put("title",   name);
                        doc.put("content", content);
                        doc.put("url",     "");
                        doc.put("ts",      java.time.Instant.now().toString());
                        docs.add(doc);
                    } catch (Exception e) {
                        logger.debug("[GContacts] Skipping contact: {}", e.getMessage());
                    }
                }
            } while (pageToken != null && !pageToken.isBlank());

            logger.info("[GContacts] Fetched {} contacts", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[GContacts] fetchDocuments failed", e);
        }
        return docs;
    }

    private String firstValue(JsonNode person, String field, String subField, String fallback) {
        JsonNode arr = person.path(field);
        if (arr.isArray() && !arr.isEmpty()) return arr.get(0).path(subField).asText(fallback);
        return fallback;
    }
}
