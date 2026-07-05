package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentron.backend.agents.MultiAgentCoordinator;
import org.opentron.backend.storage.service.StorageService;
import org.opentron.backend.storage.entities.TraceLog;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@RestController
@RequestMapping("/v1/agents")
public class AgentsController {

    private static final Logger logger = LoggerFactory.getLogger(AgentsController.class);
    private final MultiAgentCoordinator coordinator;
    
    @Autowired
    private StorageService storageService;
    
    private final Map<String, Map<String, Object>> taskResults = new ConcurrentHashMap<>();
    private final Map<String, Long> taskTimestamps = new ConcurrentHashMap<>();
    private static final long TASK_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public AgentsController(MultiAgentCoordinator coordinator) {
        this.coordinator = coordinator;
        // Start cleanup thread for old task results
        startTaskCleanupThread();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MULTI-AGENT COORDINATOR ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Process request through the agent network — streams SSE events.
     * Emits agent_start, agent_done, agent_error, and done events.
     */
    @PostMapping(value = "/coordinate", produces = "text/event-stream")
    public reactor.core.publisher.Flux<String> coordinateAgents(
            HttpServletRequest servletRequest,
            @RequestBody org.opentron.backend.dto.AgentCoordinateRequest request) {

        String userRequest = request.getRequest();
        String context = request.getContext() == null ? "" : request.getContext();

        if (userRequest == null || userRequest.isBlank()) {
            return reactor.core.publisher.Flux.just("data: {\"type\":\"error\",\"message\":\"Missing request field\"}\n\n");
        }

        final String req = userRequest;
        final String ctx = context;

        return reactor.core.publisher.Flux.create(sink -> {
            new Thread(() -> {
                long start = System.currentTimeMillis();
                try {
                    logger.info("Streaming coordinator request");

                    // Emit routing event
                    sink.next(sseEvent("status", Map.of("message", "Tron is analyzing your request...", "phase", "routing")));

                    // Delegate to coordinator (which uses virtual threads per agent)
                    // The coordinator now emits per-agent progress via a callback
                    MultiAgentCoordinator.StreamingCoordinatorCallback callback = new MultiAgentCoordinator.StreamingCoordinatorCallback() {
                        @Override
                        public void onAgentStart(String agent) {
                            sink.next(sseEvent("agent_start", Map.of("agent", agent, "message", "Agent " + agent + " is thinking...")));
                        }
                        @Override
                        public void onAgentDone(String agent, Map<String, Object> result) {
                            String preview = "";
                            if (result.containsKey("response")) {
                                String r = (String) result.get("response");
                                preview = r.length() > 120 ? r.substring(0, 120) + "..." : r;
                            }
                            sink.next(sseEvent("agent_done", Map.of("agent", agent, "preview", preview)));
                        }
                        @Override
                        public void onAgentError(String agent, String error) {
                            sink.next(sseEvent("agent_error", Map.of("agent", agent, "error", error)));
                        }
                        @Override
                        public void onStatus(String status) {
                            sink.next(sseEvent("status", Map.of("message", status)));
                        }
                    };

                    Map<String, String> apiKeys = extractApiKeysFromRequest(servletRequest);
                    Map<String, Object> result = coordinator.processRequest(req, ctx, callback, apiKeys);
                    long totalTime = System.currentTimeMillis() - start;

                    // Save trace
                    try {
                        storageService.saveTrace("coordinator", req, result != null ? result.toString() : "", (int) totalTime);
                    } catch (Exception e) {
                        logger.warn("Trace save failed", e);
                    }

                    // Emit final done event with full result
                    Map<String, Object> donePayload = new java.util.HashMap<>();
                    donePayload.put("type", "done");
                    donePayload.put("result", result);
                    donePayload.put("elapsed_ms", totalTime);
                    sink.next(sseEvent("done", donePayload));
                    sink.complete();

                } catch (Exception e) {
                    logger.error("Streaming coordinator error", e);
                    sink.next(sseEvent("error", Map.of("message", e.getMessage())));
                    sink.complete();
                }
            }).start();
        });
    }

    private Map<String, String> extractApiKeysFromRequest(HttpServletRequest request) {
        Map<String, String> apiKeys = new HashMap<>();
        try {
            String apiKeysHeader = request.getHeader("X-API-Keys");
            if (apiKeysHeader != null && !apiKeysHeader.isBlank()) {
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = objectMapper.readValue(apiKeysHeader, Map.class);
                apiKeys.putAll(parsed);
                try {
                    logger.debug("Received API keys for: {}", parsed.keySet());
                } catch (Exception e) {
                    // Defensive: logging should never fail request processing
                }
            }
        } catch (Exception e) {
            logger.warn("Could not parse API keys header", e);
        }
        return apiKeys;
    }

    private String sseEvent(String type, Map<String, Object> data) {
        try {
            Map<String, Object> payload = new java.util.HashMap<>(data);
            payload.put("type", type);
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"message\":\"serialization error\"}";
        }
    }

    /**
     * Get status of all agents
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatuses() {
        try {
            logger.info("Fetching agent statuses");
            Map<String, Object> statuses = coordinator.getAgentStatuses();
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("agents", statuses);
            response.put("timestamp", System.currentTimeMillis());
            logger.info("Returned status for {} agents", statuses != null ? statuses.size() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting agent statuses", e);
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send a task to a specific agent (async)
     * Returns a taskId for polling results
     */
        @PostMapping("/task")
        public Mono<ResponseEntity<Map<String, Object>>> sendAgentTask(
            @RequestBody org.opentron.backend.dto.AgentTaskRequest request) {
        
        return Mono.fromCallable(() -> {
            String agent = request.getAgent();
            String task = request.getTask();
            
            if (agent == null || agent.isBlank() || task == null || task.isBlank()) {
                Map<String, Object> errorMap = new java.util.HashMap<>();
                errorMap.put("error", "Missing 'agent' or 'task' field");
                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }

            // Generate unique task ID
            String taskId = "task-" + System.currentTimeMillis() + "-" + agent;
            
            logger.info("Sending task {} to agent {}", taskId, agent);

            // Store pending status
            Map<String, Object> taskData = new java.util.HashMap<>();
            taskData.put("status", "pending");
            taskData.put("agent", agent);
            taskData.put("task", task);
            taskData.put("created_at", System.currentTimeMillis());
            
            taskResults.put(taskId, taskData);
            taskTimestamps.put(taskId, System.currentTimeMillis());

            // Send message asynchronously in background
            new Thread(() -> {
                try {
                    MultiAgentCoordinator.AgentMessage msg = new MultiAgentCoordinator.AgentMessage(
                        agent,
                        "task",
                        Map.of("task", task)
                    );
                    
                    coordinator.sendMessage(msg);
                    
                    // Mark as completed (for now - in production would wait for actual result)
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("status", "completed");
                    result.put("agent", agent);
                    result.put("task", task);
                    result.put("completed_at", System.currentTimeMillis());
                    result.put("message", "Task sent to " + agent + " agent");
                    
                    taskResults.put(taskId, result);
                    
                    // Save task to database
                    try {
                        storageService.saveTrace(agent, task, "Task completed", 0);
                    } catch (Exception e) {
                        logger.warn("Failed to save task trace", e);
                    }
                } catch (Exception e) {
                    logger.error("Task execution error", e);
                    Map<String, Object> errorResult = new java.util.HashMap<>();
                    errorResult.put("status", "error");
                    errorResult.put("error", e.getMessage());
                    errorResult.put("failed_at", System.currentTimeMillis());
                    
                    taskResults.put(taskId, errorResult);
                }
            }).start();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", "pending");
            response.put("task_id", taskId);
            response.put("agent", agent);
            response.put("poll_url", "/v1/agents/task/" + taskId);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "Task queued. Poll /v1/agents/task/" + taskId + " for results");
            
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        })
        .onErrorResume(e -> {
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", e.getMessage());
            return Mono.just(new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR));
        });
    }

    /**
     * Poll for async task result
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskResult(@PathVariable String taskId) {
        try {
            Map<String, Object> result = taskResults.get(taskId);
            
            if (result == null) {
                Map<String, Object> errorMap = new java.util.HashMap<>();
                errorMap.put("error", "Task not found or expired");
                return new ResponseEntity<>(errorMap, HttpStatus.NOT_FOUND);
            }

            String status = (String) result.get("status");
            
            if ("pending".equals(status)) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("status", "pending");
                response.put("message", "Task is still processing");
                response.put("task_id", taskId);
                response.put("retry_after_ms", 1000);
                return ResponseEntity.ok(response);
            } else if ("error".equals(status)) {
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            } else if ("completed".equals(status)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            logger.error("Error polling task result", e);
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Clean up old task results (runs every minute)
     */
    private void startTaskCleanupThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 1 minute
                    
                    long now = System.currentTimeMillis();
                    taskTimestamps.entrySet().removeIf(entry -> 
                        now - entry.getValue() > TASK_TTL_MS
                    );
                    
                    logger.debug("Cleaned up old tasks. Remaining: {}", taskResults.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY AGENT ENDPOINTS (kept for backwards compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> listTemplates() {
        List<Map<String, Object>> templates = Arrays.asList(
            Map.of(
                "id", "daily-briefing",
                "name", "Daily Briefing",
                "description", "Get a morning briefing from your emails and calendar",
                "icon", "📋",
                "instruction", "Every morning, summarize my top emails and calendar for today",
                "schedule_type", "cron",
                "schedule_value", "0 9 * * *",
                "tools", Arrays.asList("email_read", "calendar_read", "web_search")
            ),
            Map.of(
                "id", "research-monitor",
                "name", "Research Monitor",
                "description", "Track the latest news and papers on a topic",
                "icon", "🔍",
                "instruction", "Search for the latest developments on AI research",
                "schedule_type", "cron",
                "schedule_value", "0 12 * * *",
                "tools", Arrays.asList("web_search", "pdf_extract")
            ),
            Map.of(
                "id", "code-reviewer",
                "name", "Code Reviewer",
                "description", "Review commits and flag bugs",
                "icon", "🔧",
                "instruction", "Review recent commits in the repo for bugs and style issues",
                "schedule_type", "manual",
                "schedule_value", null,
                "tools", Arrays.asList("git_status", "git_diff", "code_interpreter")
            ),
            Map.of(
                "id", "meeting-prep",
                "name", "Meeting Prep",
                "description", "Prepare context for your next meeting",
                "icon", "📅",
                "instruction", "Prepare a briefing for my next meeting with context from emails and notes",
                "schedule_type", "manual",
                "schedule_value", null,
                "tools", Arrays.asList("email_read", "calendar_read", "retrieval")
            )
        );
        return ResponseEntity.ok(Map.of("templates", templates));
    }

    @GetMapping("/tasks/{agentId}")
    public ResponseEntity<Map<String, Object>> listAgentTasks(@PathVariable String agentId) {
        List<Map<String, Object>> tasks = Arrays.asList(
            Map.of(
                "id", "task-1",
                "agent_id", agentId,
                "description", "Send briefing email",
                "status", "completed",
                "created_at", System.currentTimeMillis() / 1000
            ),
            Map.of(
                "id", "task-2",
                "agent_id", agentId,
                "description", "Check calendar events",
                "status", "completed",
                "created_at", System.currentTimeMillis() / 1000
            )
        );
        return ResponseEntity.ok(Map.of("tasks", tasks));
    }

    @GetMapping("/channels/{agentId}")
    public ResponseEntity<Map<String, Object>> listAgentChannels(@PathVariable String agentId) {
        List<Map<String, Object>> channels = new ArrayList<>();
        return ResponseEntity.ok(Map.of("channels", channels));
    }

    @PostMapping("/channels/{agentId}/bind")
    public ResponseEntity<Map<String, Object>> bindAgentChannel(
        @PathVariable String agentId,
        @RequestParam String type,
        @RequestBody org.opentron.backend.dto.ChannelBindRequest config
    ) {
        Map<String, Object> binding = new HashMap<>();
        binding.put("id", "binding-" + UUID.randomUUID().toString().substring(0, 8));
        binding.put("agent_id", agentId);
        binding.put("channel_type", type);
        binding.put("config", config.getConfig() == null ? new HashMap<>() : config.getConfig());
        binding.put("routing_mode", config.getRouting_mode() == null ? "default" : config.getRouting_mode());
        return ResponseEntity.ok(binding);
    }

    @DeleteMapping("/channels/{agentId}/{bindingId}")
    public ResponseEntity<Void> unbindAgentChannel(@PathVariable String agentId, @PathVariable String bindingId) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/learning-log/{agentId}")
    public ResponseEntity<Map<String, Object>> fetchLearningLog(@PathVariable String agentId) {
        List<Map<String, Object>> logs = new ArrayList<>();
        return ResponseEntity.ok(Map.of("logs", logs));
    }

    @PostMapping("/{agentId}/learning/trigger")
    public ResponseEntity<Void> triggerLearning(@PathVariable String agentId) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/traces/{agentId}")
    public ResponseEntity<Map<String, Object>> fetchAgentTraces(
        @PathVariable String agentId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> traces = new ArrayList<>();
        return ResponseEntity.ok(Map.of("traces", traces));
    }

    @GetMapping("/traces/{agentId}/{traceId}")
    public ResponseEntity<Map<String, Object>> fetchAgentTrace(
        @PathVariable String agentId,
        @PathVariable String traceId
    ) {
        Map<String, Object> trace = Map.of(
            "id", traceId,
            "agent_id", agentId,
            "steps", new ArrayList<>(),
            "outcome", "success"
        );
        return ResponseEntity.ok(trace);
    }

    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> fetchAvailableTools() {
        List<Map<String, Object>> tools = Arrays.asList(
            Map.of(
                "name", "web_search",
                "category", "search",
                "configured", true,
                "description", "Search the web"
            ),
            Map.of(
                "name", "email_read",
                "category", "communication",
                "configured", true,
                "description", "Read emails from Gmail"
            ),
            Map.of(
                "name", "calendar_read",
                "category", "knowledge",
                "configured", true,
                "description", "Read Google Calendar events"
            ),
            Map.of(
                "name", "git_status",
                "category", "vcs",
                "configured", true,
                "description", "Check Git repository status"
            ),
            Map.of(
                "name", "git_diff",
                "category", "vcs",
                "configured", true,
                "description", "Show Git diffs"
            ),
            Map.of(
                "name", "pdf_extract",
                "category", "filesystem",
                "configured", false,
                "credential_keys", Arrays.asList(),
                "description", "Extract text from PDFs"
            ),
            Map.of(
                "name", "code_interpreter",
                "category", "code",
                "configured", true,
                "description", "Execute Python code"
            ),
            Map.of(
                "name", "retrieval",
                "category", "knowledge",
                "configured", true,
                "description", "Search your knowledge base"
            )
        );
        return ResponseEntity.ok(Map.of("tools", tools));
    }

    @PostMapping("/tools/{toolName}/credentials")
    public ResponseEntity<Void> saveToolCredentials(
        @PathVariable String toolName,
        @RequestBody org.opentron.backend.dto.ToolCredentialsRequest credentials
    ) {
        return ResponseEntity.ok().build();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // DATABASE STORAGE ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Get storage statistics from PostgreSQL
     */
    @GetMapping("/storage/stats")
    public ResponseEntity<Map<String, Object>> getStorageStats() {
        try {
            StorageService.StorageStats stats = storageService.getStorageStats();
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("total_memories", stats.totalMemoryEntries);
            response.put("total_traces", stats.totalTraceEntries);
            response.put("backend", "postgresql");
            response.put("timestamp", System.currentTimeMillis());
            logger.info("Storage stats: {}", stats.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting storage stats", e);
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get recent traces for an agent from PostgreSQL
     */
    @GetMapping("/storage/traces/{agentId}")
    public ResponseEntity<Map<String, Object>> getStorageTraces(
        @PathVariable String agentId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        try {
            List<TraceLog> traces = storageService.loadTraces(agentId, Math.min(limit, 1000));
            List<Map<String, Object>> traceList = new java.util.ArrayList<>();
            
            for (TraceLog trace : traces) {
                Map<String, Object> traceMap = new java.util.HashMap<>();
                traceMap.put("id", trace.getId());
                traceMap.put("agent", trace.getAgent());
                traceMap.put("duration_ms", trace.getDurationMs());
                traceMap.put("timestamp", trace.getTimestamp().toString());
                traceMap.put("is_compressed", trace.getIsCompressed());
                traceList.add(traceMap);
            }
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("traces", traceList);
            response.put("count", traceList.size());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Loaded {} traces for agent {}", traceList.size(), agentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error loading traces for agent {}", agentId, e);
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Agent events endpoint (deprecated SSE stub).
     * Clients should use the WebSocket endpoint at `/v1/agents/events`.
     */
    @GetMapping(value = "/events", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getAgentEvents() {
        logger.info("Agent events SSE endpoint requested - advising websocket usage");
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("error", "use_websocket");
        resp.put("message", "Please connect via WebSocket to receive agent events");
        resp.put("ws_url", "/v1/agents/events?agent_id={agentId}");
        return ResponseEntity.status(426).body(resp);
    }
}
