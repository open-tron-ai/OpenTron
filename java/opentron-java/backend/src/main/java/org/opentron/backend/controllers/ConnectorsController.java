package org.opentron.backend.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/v1/connectors")
public class ConnectorsController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorsController.class);

    private final org.opentron.backend.services.OAuthService oauthService;
    private final org.opentron.backend.services.IngestService ingestService;

    public ConnectorsController(org.opentron.backend.services.OAuthService oauthService,
                                org.opentron.backend.services.IngestService ingestService) {
        this.oauthService = oauthService;
        this.ingestService = ingestService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listConnectors() {
        List<Map<String, Object>> connectors = new ArrayList<>();
        
        String[] sources = {
            "Gmail IMAP", "Slack", "Outlook", "Google Drive", 
            "Google Calendar", "Google Contacts", "Dropbox", 
            "Notion", "Obsidian", "Granola", "WhatsApp"
        };
        
        for (String source : sources) {
            Map<String, Object> connector = new HashMap<>();
            connector.put("connector_id", source.toLowerCase().replace(" ", "-"));
            connector.put("display_name", source);
            connector.put("connected", Math.random() > 0.5);
            connector.put("chunks", (int)(Math.random() * 1000));
            connectors.add(connector);
        }
        
        return ResponseEntity.ok(Map.of("connectors", connectors));
    }

    @PostMapping("/{connectorId}/connect")
    public ResponseEntity<Map<String, Object>> connectConnector(@PathVariable String connectorId) {
        return ResponseEntity.ok(Map.of(
            "status", "connected",
            "connector_id", connectorId,
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/{connectorId}/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectConnector(@PathVariable String connectorId) {
        return ResponseEntity.ok(Map.of(
            "status", "disconnected",
            "connector_id", connectorId
        ));
    }

    @GetMapping("/{connectorId}")
    public ResponseEntity<Map<String, Object>> getConnector(@PathVariable String connectorId) {
        Map<String, Object> connector = Map.of(
            "connector_id", connectorId,
            "display_name", connectorId.replace('-', ' '),
            "auth_type", "oauth",
            "connected", Math.random() > 0.5,
            "chunks", 0
        );
        return ResponseEntity.ok(connector);
    }

    @GetMapping(path = "/{connectorId}/oauth/start", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> startOAuth(@PathVariable String connectorId) {
        String html = "<html><body><h1>OAuth flow placeholder</h1>"
            + "<p>Authorization for connector <strong>" + connectorId + "</strong> is not implemented yet.</p>"
            + "<p>Close this window and check the app.</p></body></html>";
        return ResponseEntity.ok(html);
    }

    @GetMapping(path = "/{connectorId}/oauth/callback", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> oauthCallback(
        @PathVariable String connectorId,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false, name = "token_endpoint") String tokenEndpoint,
        @RequestParam(required = false, name = "client_id") String clientId,
        @RequestParam(required = false, name = "client_secret") String clientSecret,
        @RequestParam(required = false, name = "provider") String provider,
        @RequestParam(required = false, name = "redirect_uri") String redirectUri
    ) {
        logger.info("OAuth callback for connector={} code={} state={} tokenEndpoint={}", connectorId, code, state, tokenEndpoint);
        if (code == null || code.isEmpty()) {
            String html = "<html><body><h1>OAuth callback error</h1><p>Missing code.</p></body></html>";
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(html);
        }

        if (tokenEndpoint != null && !tokenEndpoint.isBlank() && clientId != null) {
            try {
                org.opentron.backend.storage.entities.OAuthToken saved = oauthService.exchangeCode(connectorId, provider == null ? "default" : provider, tokenEndpoint, clientId, clientSecret, code, redirectUri);
                String html = "<html><body><h1>Authorization complete</h1>"
                    + "<p>Token saved (id=" + saved.getId() + "). You can close this window and return to the application.</p></body></html>";
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
            } catch (Exception e) {
                logger.error("OAuth exchange failed for connector {}", connectorId, e);
                String html = "<html><body><h1>OAuth callback error</h1><p>Exchange failed.</p></body></html>";
                return ResponseEntity.status(500).contentType(MediaType.TEXT_HTML).body(html);
            }
        }

        String html = "<html><body><h1>Authorization complete</h1>"
            + "<p>Authorization code received. Complete the exchange in the backend.</p></body></html>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/upload/ingest")
    public ResponseEntity<Map<String, Object>> uploadIngest(@RequestBody org.opentron.backend.dto.ConnectorsIngestRequest request) {
        logger.info("Ingest request received for source={}", request.getSource());

        String jobId = UUID.randomUUID().toString();
        ingestService.createJob(jobId);
        Map<String, Object> payload = Map.of(
            "source", request.getSource(),
            "content", request.getContent(),
            "metadata", request.getMetadata()
        );
        ingestService.processIngest(jobId, payload);

        return ResponseEntity.ok(Map.of(
            "status", "queued",
            "job_id", jobId,
            "message", "Data ingestion scheduled",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/upload/ingest/files")
    public ResponseEntity<Map<String, Object>> uploadIngestFiles(@RequestParam(value = "files", required = false) MultipartFile[] files) {
        logger.info("Ingest files request received");
        int fileCount = files != null ? files.length : 0;

        String jobId = UUID.randomUUID().toString();
        ingestService.createJob(jobId);

        try {
            Path tempDir = Files.createTempDirectory("connectors-ingest-");
            if (files != null) {
                for (MultipartFile f : files) {
                    String name = f.getOriginalFilename();
                    if (name == null) name = UUID.randomUUID().toString();
                    Path out = tempDir.resolve(name);
                    try (InputStream in = f.getInputStream()) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            ingestService.processFileIngest(jobId, tempDir);

            return ResponseEntity.ok(Map.of(
                "status", "queued",
                "job_id", jobId,
                "message", "File ingestion scheduled",
                "files_received", fileCount,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Error saving uploaded files", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "failed to save files",
                "error", e.getMessage()
            ));
        }
    }
}
