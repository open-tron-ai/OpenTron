package org.opentron.backend.agents;

import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentron.backend.telemetry.TelemetryService;
import org.opentron.backend.util.CloudModelService;
import org.opentron.backend.util.OllamaCliService;
import org.opentron.backend.util.HuggingFaceService;
import org.opentron.backend.services.ModelSelectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Multi-Agent Coordinator System
 * Tron coordinates with specialist agents: Backend, Frontend, DevOps, QA
 */
@Component
public class MultiAgentCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(MultiAgentCoordinator.class);
    private final OllamaCliService ollamaService;
    private final HuggingFaceService huggingFaceService;
    private final CloudModelService cloudModelService;
    private final ModelSelectorService modelSelectorService;
    @Autowired(required = false)
    private TelemetryService telemetryService;
    private final Map<String, SpecializedAgent> agents = new ConcurrentHashMap<>();
    private final BlockingQueue<AgentMessage> messageQueue = new LinkedBlockingQueue<>();
    
    public SpecializedAgent getAgent(String name) {
        return agents.get(name);
    }
    
    public MultiAgentCoordinator(OllamaCliService ollamaService, HuggingFaceService huggingFaceService,
                                 ModelSelectorService modelSelectorService, CloudModelService cloudModelService) {
        this.ollamaService = ollamaService;
        this.huggingFaceService = huggingFaceService;
        this.modelSelectorService = modelSelectorService;
        this.cloudModelService = cloudModelService;
        try {
            initializeAgents();
            startMessageProcessor();
            logger.info("Initialized with intelligent model selection");
        } catch (Exception e) {
            logger.error("Initialization error", e);
            if (agents.isEmpty()) {
                initializeAgentsWithDefaults();
            }
        }
    }

    private void initializeAgentsWithDefaults() {
        logger.info("Using fallback initialization with default models");
        AgentLLMBridge llmBridge = new AgentLLMBridge(ollamaService, huggingFaceService, cloudModelService, "mistral", null);
        
        agents.put("coordinator", new CoordinatorAgent(llmBridge));
        agents.put("backend", new BackendSpecialist(llmBridge));
        agents.put("frontend", new FrontendSpecialist(llmBridge));
        agents.put("devops", new DevOpsAgent(llmBridge));
        agents.put("qa", new QAAgent(llmBridge));
    }

    private void initializeAgents() {
        initializeAgents(null);
    }

    private void initializeAgents(Map<String, String> apiKeyOverrides) {
        logger.info("Initializing with intelligent model selection...");
        
        // Select best model for each specialist
        String coordinatorModel = "mistral"; // Coordinator stays on mistral
        String backendModel = modelSelectorService.selectBestModel("backend", apiKeyOverrides);
        String frontendModel = modelSelectorService.selectBestModel("frontend", apiKeyOverrides);
        String qaModel = modelSelectorService.selectBestModel("qa", apiKeyOverrides);
        String devopsModel = modelSelectorService.selectBestModel("devops", apiKeyOverrides);
        
        logger.info("Model assignments: Backend={}, Frontend={}, QA={}, DevOps={}", backendModel, frontendModel, qaModel, devopsModel);
        
        // Create coordinator with its model
        AgentLLMBridge coordinatorBridge = new AgentLLMBridge(ollamaService, huggingFaceService, cloudModelService, coordinatorModel, apiKeyOverrides);
        agents.put("coordinator", new CoordinatorAgent(coordinatorBridge));
        
        // Create specialists with THEIR optimized models
        AgentLLMBridge backendBridge = new AgentLLMBridge(ollamaService, huggingFaceService, cloudModelService, backendModel, apiKeyOverrides);
        agents.put("backend", new BackendSpecialist(backendBridge));
        
        AgentLLMBridge frontendBridge = new AgentLLMBridge(ollamaService, huggingFaceService, cloudModelService, frontendModel, apiKeyOverrides);
        agents.put("frontend", new FrontendSpecialist(frontendBridge));
        
        AgentLLMBridge qaBridge = new AgentLLMBridge(ollamaService, huggingFaceService, cloudModelService, qaModel, apiKeyOverrides);
        agents.put("qa", new QAAgent(qaBridge));
        
        AgentLLMBridge devopsBridge = new AgentLLMBridge(ollamaService, huggingFaceService, cloudModelService, devopsModel, apiKeyOverrides);
        agents.put("devops", new DevOpsAgent(devopsBridge));
        
        logger.info("Initialized {} agents with optimized models", agents.size());
    }

    private void startMessageProcessor() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    AgentMessage msg = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        SpecializedAgent agent = agents.get(msg.targetAgent);
                        if (agent != null) {
                            long start = System.currentTimeMillis();
                            Object result = agent.process(msg);
                            long elapsed = System.currentTimeMillis() - start;
                            logger.debug("[{}] Processed in {}ms:{}", msg.targetAgent, elapsed, result);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    // Callback interface for streaming agent progress
    public interface StreamingCoordinatorCallback {
        void onAgentStart(String agent);
        void onAgentChunk(String agent, String chunk);
        void onAgentDone(String agent, Map<String, Object> result);
        void onAgentError(String agent, String error);
        void onStatus(String status);
    }

    public Map<String, Object> processRequest(String userRequest, String context) {
        return processRequest(userRequest, context, null, null);
    }

    public Map<String, Object> processRequest(String userRequest, String context, StreamingCoordinatorCallback callback) {
        return processRequest(userRequest, context, callback, null);
    }

    public Map<String, Object> processRequest(String userRequest, String context, StreamingCoordinatorCallback callback, Map<String, String> apiKeyOverrides) {
        logger.info("Processing coordinator request with overrides={}", apiKeyOverrides);
        
        long start = System.currentTimeMillis();
        
        try {
            if (apiKeyOverrides != null && !apiKeyOverrides.isEmpty()) {
                logger.info("Reinitializing agent models using API-key overrides");
                agents.clear();
                initializeAgents(apiKeyOverrides);
            } else if (agents.isEmpty()) {
                initializeAgentsWithDefaults();
            }
            
            CoordinatorAgent coordinator = (CoordinatorAgent) agents.get("coordinator");
            if (coordinator == null) {
                throw new RuntimeException("Coordinator agent not found");
            }
            
            Map<String, Object> result = coordinator.coordinate(userRequest, context, this, callback);
            long totalTime = System.currentTimeMillis() - start;
            
            Map<String, Object> response = new HashMap<>();
            response.put("result", result);
            response.put("elapsed_ms", totalTime);
            response.put("agents_used", result.get("agents_used"));
            
            logger.info("Coordinator completed in {}ms", totalTime);
            return response;
        } catch (Exception e) {
            logger.error("Coordinator error", e);
            long totalTime = System.currentTimeMillis() - start;
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("elapsed_ms", totalTime);
            response.put("status", "error");
            return response;
        }
    }

    public void sendMessage(AgentMessage msg) {
        try {
            messageQueue.offer(msg);
        } catch (Exception e) {
            logger.error("Coordinator message error", e);
        }
    }

    public Map<String, Object> getAgentStatuses() {
        try {
            if (agents.isEmpty()) {
                initializeAgentsWithDefaults();
            }
            
            Map<String, Object> statuses = new HashMap<>();
            for (Map.Entry<String, SpecializedAgent> entry : agents.entrySet()) {
                statuses.put(entry.getKey(), entry.getValue().getStatus());
            }
            return statuses;
        } catch (Exception e) {
            logger.error("Error building agent statuses", e);
            return new HashMap<>();
        }
    }

    public abstract static class SpecializedAgent {
        protected String name;
        protected List<String> skills = new ArrayList<>();
        protected long lastExecutedTime = 0;
        protected AgentLLMBridge llmBridge;

        public SpecializedAgent(AgentLLMBridge llmBridge) {
            this.llmBridge = llmBridge;
        }

        public abstract Object process(AgentMessage msg);

        protected List<String> asStringList(Object value) {
            if (value instanceof List<?> values) {
                List<String> result = new ArrayList<>();
                for (Object item : values) {
                    if (item != null) result.add(String.valueOf(item));
                }
                return result;
            }
            return List.of();
        }

        protected Map<String, Object> invokeScopedLLM(String role, String task, String context, List<String> focus, List<String> ignore, List<String> constraints, String agentKey, StreamingCoordinatorCallback callback) {
            String systemPrompt = "You are a " + role + ". Solve only the relevant domain. "
                    + "Stay concise and use the provided context. "
                    + "Focus on: " + String.join(", ", focus.isEmpty() ? List.of("the core request") : focus) + ". "
                    + (ignore.isEmpty() ? "" : "Do not address: " + String.join(", ", ignore) + ". ")
                    + (constraints.isEmpty() ? "" : "Constraints: " + String.join(", ", constraints) + ".");

            String userQuestion = "Task: " + task + "\n\nRelevant context:\n" + context + "\n\nRespond with a helpful specialist answer.";
            final StringBuilder acc = new StringBuilder();
            Map<String, Object> result = llmBridge.queryLLMStream(systemPrompt, userQuestion, 256, (chunk) -> {
                acc.append(chunk);
                if (callback != null) callback.onAgentChunk(agentKey, chunk);
            });
            // ensure the streamed text is available as `response` when possible
            if (result != null && (!result.containsKey("response") || String.valueOf(result.get("response") == null).isBlank())) {
                result.put("response", acc.toString());
            }
            result.put("agent", agentKey);
            return result;
        }

        public Map<String, Object> getStatus() {
            return Map.of(
                "name", name,
                "skills", skills,
                "last_executed_ms", lastExecutedTime
            );
        }
    }

    public static class CoordinatorAgent extends SpecializedAgent {
        public CoordinatorAgent(AgentLLMBridge llmBridge) {
            super(llmBridge);
            this.name = "Coordinator";
            this.skills = Arrays.asList(
                "analyze_requirements",
                "delegate_to_specialists",
                "monitor_progress",
                "aggregate_results",
                "error_recovery"
            );
        }

        @Override
        public Object process(AgentMessage msg) {
            return Map.of("status", "ok", "message", "Coordinator received message");
        }

        public Map<String, Object> coordinate(String userRequest, String context, MultiAgentCoordinator coordinator, StreamingCoordinatorCallback callback) {
            long start = System.currentTimeMillis();
            List<String> agentsUsed = new ArrayList<>();
            Map<String, Object> taskResults = new HashMap<>();
            
            // Record this coordination as a request
            TelemetryService ts = coordinator.telemetryService;
            if (ts != null) ts.recordRequest();

            // Send status update
            if (callback != null) callback.onStatus("Analyzing request and routing to specialist agents...");

            List<String> requiredAgents = analyzeRequest(userRequest, context);
            agentsUsed.addAll(requiredAgents);

            logger.info("Required agents: {}", requiredAgents);

            List<Future<Map<String, Object>>> futures = new ArrayList<>();
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (String agentName : requiredAgents) {
                    futures.add(executor.submit(() -> {
                        if (callback != null) callback.onAgentStart(agentName);
                        SpecializedAgent agent = coordinator.agents.get(agentName);
                        if (agent != null) {
                            List<String> focusedSkills = inferSkillsForAgent(agentName, userRequest, context);
                            Map<String, Object> specialistPayload = buildSpecialistTaskPayload(userRequest, context, agentName, focusedSkills);

                            AgentMessage msg = new AgentMessage(agentName, "task", specialistPayload, callback);
                            Map<String, Object> result = (Map<String, Object>) agent.process(msg);
                            if (callback != null) {
                                if ("error".equals(result.get("status"))) {
                                    callback.onAgentError(agentName, (String) result.getOrDefault("error", "unknown"));
                                } else {
                                    emitAgentChunks(agentName, result, callback);
                                }
                            }
                            return result;
                        }
                        if (callback != null) callback.onAgentError(agentName, "Agent not found");
                        return Map.of("error", "Agent not found");
                    }));
                }

                for (int i = 0; i < futures.size(); i++) {
                    try {
                        Map<String, Object> result = futures.get(i).get(); // no timeout — virtual thread waits
                        taskResults.put(requiredAgents.get(i), result);
                        
                        // Track tokens used by this agent
                        if (ts != null && result != null && result.containsKey("tokens_used")) {
                            Object tokensObj = result.get("tokens_used");
                            if (tokensObj instanceof Number) {
                                long tokens = ((Number) tokensObj).longValue();
                                ts.addTokens(tokens);
                                logger.debug("Tracked {} tokens from {}", tokens, requiredAgents.get(i));
                            }
                        }
                        } catch (Exception e) {
                            logger.error("{} agent error", requiredAgents.get(i), e);
                        taskResults.put(requiredAgents.get(i), Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown"));
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            String tronResponse = buildTronChatResponse(userRequest, agentsUsed, taskResults, elapsed);

            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("status", "completed");
            finalResult.put("agents_used", agentsUsed);
            finalResult.put("results", taskResults);
            finalResult.put("elapsed_ms", elapsed);
            finalResult.put("tron_response", tronResponse);
            finalResult.put("message", tronResponse);

            this.lastExecutedTime = elapsed;
            return finalResult;
        }

        private Map<String, Object> buildSpecialistTaskPayload(String userRequest, String context, String agentName, List<String> focusedSkills) {
            String normalizedContext = context == null ? "" : context.trim();
            String requestSummary = userRequest == null ? "" : userRequest.trim();
            List<String> focus = new ArrayList<>();
            List<String> ignore = new ArrayList<>();
            List<String> constraints = List.of("stay concise", "address only the relevant domain", "avoid unrelated suggestions");

            switch (agentName) {
                case "backend" -> {
                    focus.addAll(List.of("API design", "database access", "caching", "performance", "error handling"));
                    ignore.addAll(List.of("UI", "React", "CSS", "visual design"));
                }
                case "frontend" -> {
                    focus.addAll(List.of("React", "component design", "state flow", "responsive behavior", "accessibility"));
                    ignore.addAll(List.of("backend services", "database schema", "deployment"));
                }
                case "qa" -> {
                    focus.addAll(List.of("testing strategy", "debugging", "regression risks", "verification steps"));
                    ignore.addAll(List.of("visual styling", "deployment automation"));
                }
                case "devops" -> {
                    focus.addAll(List.of("monitoring", "observability", "resource usage", "reliability"));
                    ignore.addAll(List.of("UI implementation", "business logic"));
                }
                default -> {
                    focus.addAll(List.of("core request"));
                }
            }

            if (!focusedSkills.isEmpty()) {
                focus.addAll(focusedSkills);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("agent", agentName);
            payload.put("task", "Respond to the request from the perspective of the " + agentName + " specialist.");
            payload.put("request_summary", requestSummary);
            payload.put("request", requestSummary);
            payload.put("focus", focus);
            payload.put("ignore", ignore);
            payload.put("relevant_context", normalizedContext.isBlank() ? requestSummary : normalizedContext);
            payload.put("constraints", constraints);
            payload.put("domain", agentName);
            return payload;
        }

        private List<String> inferSkillsForAgent(String agentName, String request, String context) {
            String combined = (request == null ? "" : request) + " " + (context == null ? "" : context);
            String lower = combined.toLowerCase(Locale.ROOT);
            return switch (agentName) {
                case "backend" -> {
                    List<String> skills = new ArrayList<>();
                    if (lower.contains("spring") || lower.contains("java") || lower.contains("api") || lower.contains("database") || lower.contains("cache")) {
                        skills.add("spring_boot_configuration");
                        skills.add("database_query_optimization");
                    }
                    yield skills;
                }
                case "frontend" -> {
                    List<String> skills = new ArrayList<>();
                    if (lower.contains("react") || lower.contains("ui") || lower.contains("component") || lower.contains("typescript") || lower.contains("frontend")) {
                        skills.add("react_optimization");
                        skills.add("component_design");
                    }
                    yield skills;
                }
                case "qa" -> {
                    List<String> skills = new ArrayList<>();
                    if (lower.contains("test") || lower.contains("debug") || lower.contains("fix") || lower.contains("review")) {
                        skills.add("debugging");
                        skills.add("integration_testing");
                    }
                    yield skills;
                }
                case "devops" -> {
                    List<String> skills = new ArrayList<>();
                    if (lower.contains("monitor") || lower.contains("metric") || lower.contains("health") || lower.contains("deploy") || lower.contains("performance")) {
                        skills.add("performance_monitoring");
                        skills.add("health_checks");
                    }
                    yield skills;
                }
                default -> List.of();
            };
        }

        private void emitAgentChunks(String agentName, Map<String, Object> result, StreamingCoordinatorCallback callback) {
            String text = result.containsKey("response") ? String.valueOf(result.get("response")) : "";
            if (text == null || text.isBlank()) {
                callback.onAgentDone(agentName, result);
                return;
            }

            String normalized = text.trim();
            if (normalized.length() <= 1) {
                callback.onAgentChunk(agentName, normalized);
                callback.onAgentDone(agentName, result);
                return;
            }

            int chunkSize = Math.max(1, normalized.length() / 12 + 1);
            for (int i = 0; i < normalized.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, normalized.length());
                callback.onAgentChunk(agentName, normalized.substring(i, end));
            }
            callback.onAgentDone(agentName, result);
        }

        private String buildTronChatResponse(String userRequest, List<String> agentsUsed, 
                                           Map<String, Object> taskResults, long elapsedMs) {
            StringBuilder response = new StringBuilder();
            response.append("I've analyzed your request and coordinated with my specialist agents.\n\n");
            response.append("**Analysis Results:**\n\n");

            for (String agent : agentsUsed) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) taskResults.get(agent);
                if (result != null) {
                    String agentName = agent.substring(0, 1).toUpperCase() + agent.substring(1);
                    response.append("**").append(agentName).append(" Agent:**\n");
                    
                    if (result.containsKey("response")) {
                        response.append(result.get("response")).append("\n");
                    } else if (result.containsKey("error")) {
                        response.append("Error: ").append(result.get("error")).append("\n");
                    } else {
                        response.append(result.toString()).append("\n");
                    }
                    response.append("\n");
                }
            }

            response.append("*Processed by: Tron with ").append(agentsUsed.size())
                    .append(" specialist agents in ").append(elapsedMs).append("ms*");

            return response.toString();
        }

        private List<String> analyzeRequest(String request, String context) {
            List<String> agents = new ArrayList<>();
            String lower = request.toLowerCase();
            String contextLower = (context != null ? context : "").toLowerCase();
            String combinedContent = lower + " " + contextLower;

            if (combinedContent.contains("backend") || combinedContent.contains("java") || combinedContent.contains("database") || 
                combinedContent.contains("api") || combinedContent.contains("spring") || combinedContent.contains("cache")) {
                agents.add("backend");
            }

            if (combinedContent.contains("frontend") || combinedContent.contains("react") || combinedContent.contains("ui") || 
                combinedContent.contains("component") || combinedContent.contains("typescript")) {
                agents.add("frontend");
            }

            if (combinedContent.contains("test") || combinedContent.contains("debug") || combinedContent.contains("fix") || 
                combinedContent.contains("review")) {
                agents.add("qa");
            }

            if (combinedContent.contains("monitor") || combinedContent.contains("metrics") || combinedContent.contains("health")) {
                agents.add("devops");
            }

            if (agents.isEmpty()) {
                agents.add("backend");
                agents.add("frontend");
            }

            return agents;
        }
    }

    public static class BackendSpecialist extends SpecializedAgent {
        public BackendSpecialist(AgentLLMBridge llmBridge) {
            super(llmBridge);
            this.name = "Backend";
            this.skills = Arrays.asList(
                "java_optimization",
                "spring_boot_configuration",
                "database_query_optimization",
                "api_design",
                "persistence_layer",
                "cache_optimization",
                "concurrent_programming",
                "error_handling"
            );
        }

        @Override
        public Object process(AgentMessage msg) {
            long start = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) msg.payload;
            String task = String.valueOf(payload.getOrDefault("task", payload.getOrDefault("request", "")));
            String context = String.valueOf(payload.getOrDefault("relevant_context", payload.getOrDefault("context", "")));
            List<String> focus = asStringList(payload.get("focus"));
            List<String> ignore = asStringList(payload.get("ignore"));
            List<String> constraints = asStringList(payload.get("constraints"));

            Map<String, Object> result = invokeScopedLLM("backend optimization expert", task, context, focus, ignore, constraints, "backend", msg.streamCallback);
            this.lastExecutedTime = System.currentTimeMillis() - start;
            return result;
        }
    }

    public static class FrontendSpecialist extends SpecializedAgent {
        public FrontendSpecialist(AgentLLMBridge llmBridge) {
            super(llmBridge);
            this.name = "Frontend";
            this.skills = Arrays.asList(
                "react_optimization",
                "component_design",
                "state_management",
                "performance_tuning",
                "css_optimization",
                "accessibility",
                "responsive_design",
                "bundle_optimization"
            );
        }

        @Override
        public Object process(AgentMessage msg) {
            long start = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) msg.payload;
            String task = String.valueOf(payload.getOrDefault("task", payload.getOrDefault("request", "")));
            String context = String.valueOf(payload.getOrDefault("relevant_context", payload.getOrDefault("context", "")));
            List<String> focus = asStringList(payload.get("focus"));
            List<String> ignore = asStringList(payload.get("ignore"));
            List<String> constraints = asStringList(payload.get("constraints"));

            Map<String, Object> result = invokeScopedLLM("frontend React expert", task, context, focus, ignore, constraints, "frontend", msg.streamCallback);
            this.lastExecutedTime = System.currentTimeMillis() - start;
            return result;
        }
    }

    public static class DevOpsAgent extends SpecializedAgent {
        public DevOpsAgent(AgentLLMBridge llmBridge) {
            super(llmBridge);
            this.name = "DevOps";
            this.skills = Arrays.asList(
                "performance_monitoring",
                "log_aggregation",
                "metrics_collection",
                "alerting",
                "health_checks",
                "capacity_planning",
                "resource_optimization",
                "skill_synchronization"
            );
        }

        @Override
        public Object process(AgentMessage msg) {
            long start = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) msg.payload;
            String task = String.valueOf(payload.getOrDefault("task", payload.getOrDefault("request", "")));
            String context = String.valueOf(payload.getOrDefault("relevant_context", payload.getOrDefault("context", "")));
            List<String> focus = asStringList(payload.get("focus"));
            List<String> ignore = asStringList(payload.get("ignore"));
            List<String> constraints = asStringList(payload.get("constraints"));

            Map<String, Object> result = invokeScopedLLM("DevOps monitoring expert", task, context, focus, ignore, constraints, "devops", msg.streamCallback);
            this.lastExecutedTime = System.currentTimeMillis() - start;
            return result;
        }
    }

    public static class QAAgent extends SpecializedAgent {
        public QAAgent(AgentLLMBridge llmBridge) {
            super(llmBridge);
            this.name = "QA";
            this.skills = Arrays.asList(
                "unit_testing",
                "integration_testing",
                "debugging",
                "code_review",
                "regression_testing",
                "performance_testing",
                "security_testing",
                "compatibility_testing"
            );
        }

        @Override
        public Object process(AgentMessage msg) {
            long start = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) msg.payload;
            String task = String.valueOf(payload.getOrDefault("task", payload.getOrDefault("request", "")));
            String context = String.valueOf(payload.getOrDefault("relevant_context", payload.getOrDefault("context", "")));
            List<String> focus = asStringList(payload.get("focus"));
            List<String> ignore = asStringList(payload.get("ignore"));
            List<String> constraints = asStringList(payload.get("constraints"));

            Map<String, Object> result = invokeScopedLLM("QA testing expert", task, context, focus, ignore, constraints, "qa", msg.streamCallback);
            this.lastExecutedTime = System.currentTimeMillis() - start;
            return result;
        }
    }

    public static class AgentMessage {
        public String targetAgent;
        public String type;
        public Object payload;
        public String replyTo;
        public StreamingCoordinatorCallback streamCallback;
        public long timestamp = System.currentTimeMillis();

        public AgentMessage(String targetAgent, String type, Object payload) {
            this(targetAgent, type, payload, (String) null, null);
        }

        public AgentMessage(String targetAgent, String type, Object payload, String replyTo) {
            this(targetAgent, type, payload, replyTo, null);
        }

        public AgentMessage(String targetAgent, String type, Object payload, StreamingCoordinatorCallback streamCallback) {
            this(targetAgent, type, payload, null, streamCallback);
        }

        public AgentMessage(String targetAgent, String type, Object payload, String replyTo, StreamingCoordinatorCallback streamCallback) {
            this.targetAgent = targetAgent;
            this.type = type;
            this.payload = payload;
            this.replyTo = replyTo;
            this.streamCallback = streamCallback;
        }
    }
}
