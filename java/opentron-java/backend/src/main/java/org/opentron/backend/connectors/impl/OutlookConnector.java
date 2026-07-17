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
 * Outlook connector — Microsoft Graph API v1.0.
 * credentialToken = Microsoft OAuth2 access token (Mail.Read scope).
 *
 * Fetches messages from all mail folders, up to 500 per sync.
 * The credential is obtained via the OAuth flow already wired in ConnectorsController:
 * the user provides Client ID + Client Secret, the server exchanges for an access token,
 * and that token is stored as credentialToken in ConnectorState.
 */
@Component
public class OutlookConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(OutlookConnector.class);
    private static final String BASE = "https://graph.microsoft.com/v1.0";
    private static final int MAX_MESSAGES = 500;

    private final WebClient client = WebClient.create(BASE);
    private final ObjectMapper mapper = new ObjectMapper();
    private String lastError;

    @Override public String id()        { return "outlook"; }
    @Override public String name()      { return "Outlook"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Microsoft OAuth access token required"; return false;
        }
        try {
            String body = client.get().uri("/me")
                .header("Authorization", "Bearer " + credentialToken.trim())
                .retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(body);
            if (root.has("error")) {
                lastError = root.path("error").path("message").asText("Auth failed");
                return false;
            }
            logger.info("[Outlook] Connected as {}", root.path("userPrincipalName").asText("unknown"));
            return true;
        } catch (Exception e) { lastError = e.getMessage(); return false; }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String token = credentialToken.trim();

        try {
            // List all mail folders
            String foldersResp = client.get()
                .uri("/me/mailFolders?$top=50&$select=id,displayName,totalItemCount")
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(String.class).block();

            JsonNode foldersRoot = mapper.readTree(foldersResp);
            if (foldersRoot.has("error")) {
                lastError = foldersRoot.path("error").path("message").asText();
                return docs;
            }

            for (JsonNode folder : foldersRoot.path("value")) {
                String folderId   = folder.path("id").asText();
                String folderName = folder.path("displayName").asText("Folder");
                int    itemCount  = folder.path("totalItemCount").asInt(0);
                if (itemCount == 0) continue;

                try {
                    String filter = since != null
                        ? "$filter=receivedDateTime ge " + since.replace(":", "%3A") : "";
                    String msgResp = client.get()
                        .uri("/me/mailFolders/" + folderId + "/messages"
                            + "?$top=" + MAX_MESSAGES
                            + "&$select=id,subject,from,receivedDateTime,bodyPreview,webLink"
                            + "&$orderby=receivedDateTime desc"
                            + (filter.isEmpty() ? "" : "&" + filter))
                        .header("Authorization", "Bearer " + token)
                        .retrieve().bodyToMono(String.class).block();

                    JsonNode msgRoot = mapper.readTree(msgResp);
                    for (JsonNode msg : msgRoot.path("value")) {
                        try {
                            String msgId    = msg.path("id").asText();
                            String subject  = msg.path("subject").asText("(no subject)");
                            String from     = msg.path("from").path("emailAddress")
                                               .path("address").asText("");
                            String fromName = msg.path("from").path("emailAddress")
                                               .path("name").asText(from);
                            String preview  = msg.path("bodyPreview").asText("");
                            String received = msg.path("receivedDateTime").asText(
                                              java.time.Instant.now().toString());
                            String webLink  = msg.path("webLink").asText("");

                            String content = "Subject: " + subject
                                + "\nFrom: " + fromName + " <" + from + ">"
                                + "\nFolder: " + folderName
                                + "\nReceived: " + received
                                + "\n\n" + preview;

                            Map<String, Object> doc = new HashMap<>();
                            doc.put("id",      "outlook:" + msgId);
                            doc.put("title",   "From " + fromName + ": " + subject);
                            doc.put("content", content);
                            doc.put("url",     webLink);
                            doc.put("ts",      received);
                            docs.add(doc);
                        } catch (Exception e) {
                            logger.debug("[Outlook] Skipping message: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.debug("[Outlook] Skipping folder {}: {}", folderName, e.getMessage());
                }
            }
            logger.info("[Outlook] Fetched {} messages", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[Outlook] fetchDocuments failed", e);
        }
        return docs;
    }
}
