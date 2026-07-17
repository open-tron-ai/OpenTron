package org.opentron.backend.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.opentron.backend.storage.entities.ConnectorState;
import org.opentron.backend.storage.repositories.ConnectorStateRepository;
import org.opentron.backend.storage.service.ConnectorStateService;
import org.opentron.backend.services.IngestService;
import org.opentron.backend.services.OAuthService;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/v1/connectors")
public class ConnectorsController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorsController.class);

    // OAuth config: connector_id -> { auth_url, token_url, scopes }
    private static final Map<String, Map<String, String>> OAUTH_CONFIG = Map.of(
        "gdrive", Map.of(
            "auth_url",  "https://accounts.google.com/o/oauth2/v2/auth",
            "token_url", "https://oauth2.googleapis.com/token",
            "scopes",    "https://www.googleapis.com/auth/drive.readonly"
        ),
        "gcalendar", Map.of(
            "auth_url",  "https://accounts.google.com/o/oauth2/v2/auth",
            "token_url", "https://oauth2.googleapis.com/token",
            "scopes",    "https://www.googleapis.com/auth/calendar.readonly"
        ),
        "gcontacts", Map.of(
            "auth_url",  "https://accounts.google.com/o/oauth2/v2/auth",
            "token_url", "https://oauth2.googleapis.com/token",
            "scopes",    "https://www.googleapis.com/auth/contacts.readonly"
        ),
        "outlook", Map.of(
            "auth_url",  "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
            "token_url", "https://login.microsoftonline.com/common/oauth2/v2.0/token",
            "scopes",    "https://graph.microsoft.com/Mail.Read https://graph.microsoft.com/Calendars.Read offline_access"
        ),
        "dropbox", Map.of(
            "auth_url",  "https://www.dropbox.com/oauth2/authorize",
            "token_url", "https://api.dropboxapi.com/oauth2/token",
            "scopes",    ""
        )
    );

    @Value("${app.base-url:http://localhost:8000}")
    private String appBaseUrl;

    private final ConnectorStateService connectorStateService;
    private final ConnectorStateRepository connectorStateRepo;
    private final OAuthService oauthService;
    private final IngestService ingestService;

    public ConnectorsController(ConnectorStateService connectorStateService,
                                ConnectorStateRepository connectorStateRepo,
                                OAuthService oauthService,
                                IngestService ingestService) {
        this.connectorStateService = connectorStateService;
        this.connectorStateRepo    = connectorStateRepo;
        this.oauthService          = oauthService;
        this.ingestService         = ingestService;
    }

    // ── List ──────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Map<String, Object>> listConnectors() {
        return ResponseEntity.ok(Map.of("connectors", connectorStateService.listConnectors()));
    }

    // ── Get single connector ──────────────────────────────────────────────

    @GetMapping("/{connectorId}")
    public ResponseEntity<Map<String, Object>> getConnector(@PathVariable String connectorId) {
        return connectorStateService.listConnectors().stream()
            .filter(c -> connectorId.equals(c.get("connector_id")))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Connect ───────────────────────────────────────────────────────────

    @PostMapping("/{connectorId}/connect")
    public ResponseEntity<Map<String, Object>> connectConnector(
            @PathVariable String connectorId,
            @RequestBody(required = false) Map<String, Object> body) {

        if (body == null) body = Map.of();

        String clientId     = str(body, "clientId", "client_id");
        String clientSecret = str(body, "clientSecret", "client_secret");
        String token        = str(body, "token");
        String path         = str(body, "path");
        String code         = str(body, "code");
        String email        = str(body, "email");
        String password     = str(body, "password");

        // OAuth connectors: Client ID + Secret -> persist to DB, return consent URL
        if (clientId != null && !clientId.isBlank() && OAUTH_CONFIG.containsKey(connectorId)) {
            Map<String, String> cfg = OAUTH_CONFIG.get(connectorId);

            // Persist the credentials in ConnectorState so they survive restarts
            ConnectorState state = connectorStateRepo.findById(connectorId)
                .orElse(new ConnectorState(connectorId, connectorId));
            state.setPendingClientId(clientId);
            state.setPendingClientSecret(clientSecret != null ? clientSecret : "");
            state.setPendingTokenUrl(cfg.get("token_url"));
            state.setSyncState("oauth_pending");
            connectorStateRepo.save(state);

            String redirectUri = buildRedirectUri(connectorId);
            String oauthStart  = buildAuthUrl(
                cfg.get("auth_url"), clientId, cfg.get("scopes"), redirectUri, connectorId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("connector_id", connectorId);
            resp.put("connected",    false);
            resp.put("status",       "oauth_required");
            resp.put("oauth_start",  oauthStart);
            return ResponseEntity.ok(resp);
        }

        // Non-OAuth connectors: direct credential
        String credential = null;
        if (token    != null && !token.isBlank())   credential = token;
        else if (path != null && !path.isBlank())   credential = path;
        else if (code != null && !code.isBlank())   credential = code;
        else if (email != null && password != null) credential = email + ":" + password;

        Map<String, Object> result = connectorStateService.connect(
                connectorId, credential != null ? credential : "");
        int status = "error".equals(result.get("status")) ? 400 : 200;
        return ResponseEntity.status(status).body(result);
    }

    // ── Disconnect ────────────────────────────────────────────────────────

    @PostMapping("/{connectorId}/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectConnector(@PathVariable String connectorId) {
        connectorStateService.disconnect(connectorId);
        return ResponseEntity.ok(Map.of("status", "disconnected", "connector_id", connectorId));
    }

    // ── Sync status ───────────────────────────────────────────────────────

    @GetMapping("/{connectorId}/sync")
    public ResponseEntity<Map<String, Object>> getSyncStatus(@PathVariable String connectorId) {
        return ResponseEntity.ok(connectorStateService.getSyncStatus(connectorId));
    }

    @PostMapping("/{connectorId}/sync")
    public ResponseEntity<Map<String, Object>> triggerSync(@PathVariable String connectorId) {
        Map<String, Object> result = connectorStateService.triggerSync(connectorId);
        int status = "error".equals(result.get("status")) ? 400 : 200;
        return ResponseEntity.status(status).body(result);
    }

    // ── OAuth start (informational only) ─────────────────────────────────

    @GetMapping(path = "/{connectorId}/oauth/start", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> startOAuth(@PathVariable String connectorId) {
        return html("<h1>OAuth</h1><p>Use the Connect dialog to paste your Client ID and Secret.</p>", 200);
    }

    // ── OAuth callback ────────────────────────────────────────────────────

    @GetMapping(path = "/{connectorId}/oauth/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> oauthCallback(
            @PathVariable String connectorId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        logger.info("OAuth callback connector={} state={} error={}", connectorId, state, error);

        if (error != null && !error.isBlank()) {
            return html("<h1>Authorization denied</h1><p>" + escape(error)
                + "</p><p>Close this window and try again.</p>", 400);
        }
        if (code == null || code.isBlank()) {
            return html("<h1>Error</h1><p>Missing authorization code.</p>", 400);
        }

        // Load credentials from DB — survives server restarts
        ConnectorState pending = connectorStateRepo.findById(connectorId).orElse(null);

        if (pending != null && pending.getPendingClientId() != null) {
            String clientId     = pending.getPendingClientId();
            String clientSecret = pending.getPendingClientSecret();
            String tokenUrl     = pending.getPendingTokenUrl();
            String redirectUri  = buildRedirectUri(connectorId);

            try {
                var saved = oauthService.exchangeCode(
                        connectorId, connectorId, tokenUrl,
                        clientId, clientSecret, code, redirectUri);

                // Clear pending fields and mark connected
                pending.setPendingClientId(null);
                pending.setPendingClientSecret(null);
                pending.setPendingTokenUrl(null);
                pending.setSyncState("idle");
                connectorStateRepo.save(pending);

                // connect() validates the token and sets connected=true
                Map<String, Object> result = connectorStateService.connect(
                        connectorId, saved.getAccessToken());

                if (Boolean.TRUE.equals(result.get("connected"))) {
                    logger.info("[OAuth] {} connected successfully", connectorId);
                    return html("<h1>Connected!</h1><p>Authorization complete. "
                        + "This window will close automatically.</p>"
                        + "<script>function closePopup(){window.close(); window.open('','_self').close();}"
                        + "setTimeout(closePopup,1500);"
                        + "document.addEventListener('click', closePopup);</script>", 200);
                } else {
                    String detail = (String) result.getOrDefault("detail", "Validation failed");
                    logger.warn("[OAuth] {} token exchange ok but connect() failed: {}", connectorId, detail);
                    return html("<h1>Error</h1><p>Token obtained but validation failed: "
                        + escape(detail) + "</p>", 500);
                }
            } catch (Exception e) {
                logger.error("OAuth token exchange failed for {}", connectorId, e);
                return html("<h1>Error</h1><p>Token exchange failed: "
                    + escape(e.getMessage()) + "</p>", 500);
            }
        }

        logger.warn("[OAuth] No pending credentials in DB for connector={}", connectorId);
        return html("<h1>Authorization received</h1>"
            + "<p>No pending credentials found. Please try connecting again.</p>", 400);
    }

    // ── Upload / Paste ingest ─────────────────────────────────────────────

    @PostMapping("/upload/ingest")
    public ResponseEntity<Map<String, Object>> uploadIngest(
            @RequestBody org.opentron.backend.dto.ConnectorsIngestRequest request) {
        String jobId = UUID.randomUUID().toString();
        ingestService.createJob(jobId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("source",   request.getSource());
        payload.put("content",  request.getContent());
        payload.put("metadata", request.getMetadata());
        ingestService.processIngest(jobId, payload);
        return ResponseEntity.ok(Map.of(
            "status", "queued", "job_id", jobId,
            "chunks_added", 1, "message", "Data ingestion scheduled",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/upload/ingest/files")
    public ResponseEntity<Map<String, Object>> uploadIngestFiles(
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
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
                "status", "queued", "job_id", jobId,
                "files_received", fileCount, "chunks_added", fileCount * 5,
                "message", "File ingestion scheduled",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Error saving uploaded files", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error", "message", "failed to save files", "error", e.getMessage()
            ));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String str(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return null;
    }

    private String buildRedirectUri(String connectorId) {
        return appBaseUrl + "/v1/connectors/" + connectorId + "/oauth/callback";
    }

    private String buildAuthUrl(String authUrl, String clientId, String scopes,
                                String redirectUri, String state) {
        return authUrl
            + "?client_id="     + encode(clientId)
            + "&redirect_uri="  + encode(redirectUri)
            + "&response_type=code"
            + "&scope="         + encode(scopes)
            + "&state="         + encode(state)
            + "&access_type=offline"
            + "&prompt=consent";
    }

    private String encode(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private String escape(String v) {
        return v == null ? "" : v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private ResponseEntity<String> html(String body, int status) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML)
            .body("<html><body>" + body + "</body></html>");
    }
}
