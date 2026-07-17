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
 * Google Calendar connector — Calendar REST API v3.
 * credentialToken = Google OAuth2 access token.
 * Fetches events from all calendars for the past 90 days + next 30 days by default,
 * or from `since` when an incremental sync is available.
 */
@Component
public class GCalendarConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(GCalendarConnector.class);
    private static final String BASE = "https://www.googleapis.com";

    private final WebClient client = WebClient.create(BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "gcalendar"; }
    @Override public String name()      { return "Google Calendar"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Google OAuth access token required"; return false;
        }
        try {
            String body = client.get().uri("/calendar/v3/users/me/calendarList")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("error")) { lastError = root.path("error").path("message").asText(); return false; }
            logger.info("[GCalendar] Connected, {} calendars", root.path("items").size());
            return true;
        } catch (Exception e) { lastError = e.getMessage(); return false; }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String token = credentialToken.trim();

        // Default window: 90 days ago → 30 days ahead
        String timeMin = since != null ? since
            : java.time.Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS).toString();
        String timeMax = java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS).toString();

        try {
            // List all calendars
            String calResp = client.get().uri("/calendar/v3/users/me/calendarList")
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(String.class).block();

            JsonNode calRoot = mapper.readTree(calResp);
            for (JsonNode cal : calRoot.path("items")) {
                String calId   = cal.path("id").asText();
                String calName = cal.path("summary").asText("Calendar");
                try {
                    String evResp = client.get()
                        .uri(u -> u.path("/calendar/v3/calendars/" + calId + "/events")
                            .queryParam("timeMin", timeMin)
                            .queryParam("timeMax", timeMax)
                            .queryParam("singleEvents", true)
                            .queryParam("maxResults", 500)
                            .queryParam("orderBy", "startTime").build())
                        .header("Authorization", "Bearer " + token)
                        .retrieve().bodyToMono(String.class).block();

                    JsonNode evRoot = mapper.readTree(evResp);
                    for (JsonNode ev : evRoot.path("items")) {
                        String evId      = ev.path("id").asText();
                        String summary   = ev.path("summary").asText("(no title)");
                        String desc      = ev.path("description").asText("");
                        String location  = ev.path("location").asText("");
                        String start     = ev.path("start").path("dateTime")
                                            .asText(ev.path("start").path("date").asText());
                        String htmlLink  = ev.path("htmlLink").asText("");
                        String attendees = buildAttendees(ev);

                        String content = "Event: " + summary
                            + (location.isBlank() ? "" : "\nLocation: " + location)
                            + "\nStart: " + start
                            + (attendees.isBlank() ? "" : "\nAttendees: " + attendees)
                            + (desc.isBlank() ? "" : "\nDescription: " + desc);

                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id",      "gcal:" + calId + ":" + evId);
                        doc.put("title",   "[" + calName + "] " + summary);
                        doc.put("content", content);
                        doc.put("url",     htmlLink);
                        doc.put("ts",      start.isEmpty() ? java.time.Instant.now().toString() : start);
                        docs.add(doc);
                    }
                } catch (Exception e) {
                    logger.debug("[GCalendar] Skipping calendar {}: {}", calId, e.getMessage());
                }
            }
            logger.info("[GCalendar] Fetched {} events", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[GCalendar] fetchDocuments failed", e);
        }
        return docs;
    }

    private String buildAttendees(JsonNode ev) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode att : ev.path("attendees")) {
            if (sb.length() > 0) sb.append(", ");
            String display = att.path("displayName").asText("");
            String email   = att.path("email").asText("");
            sb.append(display.isBlank() ? email : display);
        }
        return sb.toString();
    }
}
