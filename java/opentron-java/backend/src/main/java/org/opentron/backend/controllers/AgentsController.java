package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.opentron.backend.agents.MultiAgentCoordinator;
import org.opentron.backend.storage.service.StorageService;
import org.opentron.backend.storage.entities.TraceLog;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@RestController
@RequestMapping("/v1/agents")
public class AgentsController {

    private final MultiAgentCoordinator coordinator;
    
    @Autowired
    private StorageService storageService;
    
    private final Map<String, Map<String, Object>> taskResults = new ConcurrentHashMap<>();
    private final Map<String, Long> taskTimestamps = new ConcurrentHashMap<>();
    private static final long TASK_TTL_MS = 5 * 60 * 1000; // 5 minutes

    public AgentsController(MultiAgentCoordinator coordinator) {
        this.coordinator = coordinator;
        // Start cleanup thread for old task results
        startTaskCleanupThread();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MULTI-AGENT COORDINATOR ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Process request through the agent network (blocking call)
     * Coordinator automatically delegates to appropriate specialists
     */
    @PostMapping("/coordinate")
    public Mono<ResponseEntity<Map<String, Object>>> coordinateAgents(
            @RequestBody Map<String, String> request) {
        
        return Mono.fromCallable(() -> {
            String userRequest = request.get("request");
            String context = request.getOrDefault("context", "");
            
            if (userRequest == null || userRequest.isBlank()) {
                Map<String, Object> errorMap = new java.util.HashMap<>();
                errorMap.put("error", "Missing 'request' field");
                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }

            System.out.println("[AgentsController] 🚀 Processing: " + userRequest);
            long start = System.currentTimeMillis();

            try {
                Map<String, Object> result = coordinator.processRequest(userRequest, context);
                long totalTime = System.currentTimeMillis() - start;
                
                // Save trace to PostgreSQL
                try {
                    String resultStr = result != null ? result.toString() : "no result";
                    storageService.saveTrace("coordinator", userRequest, resultStr, (int) totalTime);
                    System.out.println("[AgentsController] 💾 Trace saved to PostgreSQL");
                } catch (Exception e) {
                    System.err.println("[AgentsController] ⚠️ Failed to save trace: " + e.getMessage());
                }
                
                // Ensure response shape matches frontend expectations
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("result", result);
                response.put("elapsed_ms", totalTime);
                response.put("total_time_ms", totalTime);
                response.put("timestamp", System.currentTimeMillis());
                
                System.out.println("[AgentsController] ✅ Response ready in " + totalTime + "ms");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - start;
                System.err.println("[AgentsController] ❌ Error: " + e.getMessage());
                e.printStackTrace();
                
                Map<String, Object> errorMap = new java.util.HashMap<>();
                errorMap.put("error", e.getMessage() != null ? e.getMessage() : "Coordinator error");
                errorMap.put("elapsed_ms", totalTime);
                
                return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        })
        .retryWhen(Retry.backoff(2, java.time.Duration.ofMillis(100)))
        .onErrorResume(e -> {
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "Coordinator timeout or failure: " + e.getMessage());
            return Mono.just(new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR));
        });
    }

    /**
     * Get status of all agents
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatuses() {
        try {
            System.out.println("[AgentsController] 📊 Fetching agent statuses...");
            Map<String, Object> statuses = coordinator.getAgentStatuses();
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("agents", statuses);
            response.put("timestamp", System.currentTimeMillis());
            System.out.println("[AgentsController] ✅ Returned status for " + (statuses != null ? statuses.size() : 0) + " agents");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[AgentsController] ❌ Error getting statuses: " + e.getMessage());
            e.printStackTrace();
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
            @RequestBody Map<String, String> request) {
        
        return Mono.fromCallable(() -> {
            String agent = request.get("agent");
            String task = request.get("task");
            
            if (agent == null || agent.isBlank() || task == null || task.isBlank()) {
                Map<String, Object> errorMap = new java.util.HashMap<>();
                errorMap.put("error", "Missing 'agent' or 'task' field");
                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }

            // Generate unique task ID
            String taskId = "task-" + System.currentTimeMillis() + "-" + agent;
            
            System.out.println("[AgentsController] 📤 Sending task " + taskId + " to " + agent);

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
                        System.err.println("[AgentsController] ⚠️ Failed to save task trace: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("[AgentsController] Task error: " + e.getMessage());
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
            System.err.println("[AgentsController] Error polling task: " + e.getMessage());
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
                    
                    System.out.println("[AgentsController] 🧹 Cleaned up old tasks. Remaining: " + 
                        taskResults.size());
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
        @RequestBody Map<String, Object> config
    ) {
        Map<String, Object> binding = new HashMap<>();
        binding.put("id", "binding-" + UUID.randomUUID().toString().substring(0, 8));
        binding.put("agent_id", agentId);
        binding.put("channel_type", type);
        binding.put("config", config);
        binding.put("routing_mode", "default");
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
        @RequestBody Map<String, Object> credentials
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
            System.out.println("[AgentsController] 📊 Storage stats: " + stats.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[AgentsController] ❌ Error getting storage stats: " + e.getMessage());
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
            
            System.out.println("[AgentsController] 📋 Loaded " + traceList.size() + " traces for agent " + agentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[AgentsController] ❌ Error loading traces: " + e.getMessage());
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
