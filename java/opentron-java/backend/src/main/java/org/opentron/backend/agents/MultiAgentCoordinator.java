package org.opentron.backend.agents;

import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentron.backend.telemetry.TelemetryService;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OllamaCliService ollamaService;
    private final HuggingFaceService huggingFaceService;
    private final ModelSelectorService modelSelectorService;
    @Autowired(required = false)
    private TelemetryService telemetryService;
    private final Map<String, SpecializedAgent> agents = new ConcurrentHashMap<>();
    private final BlockingQueue<AgentMessage> messageQueue = new LinkedBlockingQueue<>();
    
    public SpecializedAgent getAgent(String name) {
        return agents.get(name);
    }
    
    public MultiAgentCoordinator(OllamaCliService ollamaService, HuggingFaceService huggingFaceService, ModelSelectorService modelSelectorService) {
        this.ollamaService = ollamaService;
        this.huggingFaceService = huggingFaceService;
        this.modelSelectorService = modelSelectorService;
        try {
            initializeAgents();
            startMessageProcessor();
            System.out.println("[MultiAgentCoordinator] OK - Initialized with intelligent model selection");
        } catch (Exception e) {
            System.err.println("[MultiAgentCoordinator] Init error: " + e.getMessage());
            if (agents.isEmpty()) {
                initializeAgentsWithDefaults();
            }
        }
    }

    private void initializeAgentsWithDefaults() {
        System.out.println("[MultiAgentCoordinator] Using fallback initialization with default models");
        AgentLLMBridge llmBridge = new AgentLLMBridge(ollamaService, huggingFaceService, "mistral");
        
        agents.put("coordinator", new CoordinatorAgent(llmBridge));
        agents.put("backend", new BackendSpecialist(llmBridge));
        agents.put("frontend", new FrontendSpecialist(llmBridge));
        agents.put("devops", new DevOpsAgent(llmBridge));
        agents.put("qa", new QAAgent(llmBridge));
    }

    private void initializeAgents() {
        System.out.println("[MultiAgentCoordinator] Initializing with intelligent model selection...");
        
        // Select best model for each specialist
        String coordinatorModel = "mistral"; // Coordinator stays on mistral
        String backendModel = modelSelectorService.selectBestModel("backend");
        String frontendModel = modelSelectorService.selectBestModel("frontend");
        String qaModel = modelSelectorService.selectBestModel("qa");
        String devopsModel = modelSelectorService.selectBestModel("devops");
        
        System.out.println("[MultiAgentCoordinator] Model assignments:");
        System.out.println("  Backend: " + backendModel);
        System.out.println("  Frontend: " + frontendModel);
        System.out.println("  QA: " + qaModel);
        System.out.println("  DevOps: " + devopsModel);
        
        // Create coordinator with its model
        AgentLLMBridge coordinatorBridge = new AgentLLMBridge(ollamaService, huggingFaceService, coordinatorModel);
        agents.put("coordinator", new CoordinatorAgent(coordinatorBridge));
        
        // Create specialists with THEIR optimized models
        AgentLLMBridge backendBridge = new AgentLLMBridge(ollamaService, huggingFaceService, backendModel);
        agents.put("backend", new BackendSpecialist(backendBridge));
        
        AgentLLMBridge frontendBridge = new AgentLLMBridge(ollamaService, huggingFaceService, frontendModel);
        agents.put("frontend", new FrontendSpecialist(frontendBridge));
        
        AgentLLMBridge qaBridge = new AgentLLMBridge(ollamaService, huggingFaceService, qaModel);
        agents.put("qa", new QAAgent(qaBridge));
        
        AgentLLMBridge devopsBridge = new AgentLLMBridge(ollamaService, huggingFaceService, devopsModel);
        agents.put("devops", new DevOpsAgent(devopsBridge));
        
        System.out.println("[MultiAgentCoordinator] Initialized " + agents.size() + " agents with optimized models");
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
                            System.out.println("[" + msg.targetAgent + "] Processed in " + elapsed + "ms");
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
        void onAgentDone(String agent, Map<String, Object> result);
        void onAgentError(String agent, String error);
    }

    public Map<String, Object> processRequest(String userRequest, String context) {
        return processRequest(userRequest, context, null);
    }

    public Map<String, Object> processRequest(String userRequest, String context, StreamingCoordinatorCallback callback) {
        System.out.println("[Coordinator] Processing: " + userRequest);
        
        long start = System.currentTimeMillis();
        
        try {
            if (agents.isEmpty()) {
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
            
            System.out.println("[Coordinator] Completed in " + totalTime + "ms");
            return response;
        } catch (Exception e) {
            System.err.println("[Coordinator] Error: " + e.getMessage());
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
            System.err.println("[Coordinator] Message error: " + e.getMessage());
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
            System.err.println("[Coordinator] Status error: " + e.getMessage());
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

            List<String> requiredAgents = analyzeRequest(userRequest);
            agentsUsed.addAll(requiredAgents);

            System.out.println("[Coordinator] Required agents: " + requiredAgents);

            List<Future<Map<String, Object>>> futures = new ArrayList<>();
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (String agentName : requiredAgents) {
                    futures.add(executor.submit(() -> {
                        if (callback != null) callback.onAgentStart(agentName);
                        SpecializedAgent agent = coordinator.agents.get(agentName);
                        if (agent != null) {
                            AgentMessage msg = new AgentMessage(
                                agentName, "task",
                                Map.of("request", userRequest, "context", context)
                            );
                            Map<String, Object> result = (Map<String, Object>) agent.process(msg);
                            if (callback != null) {
                                if ("error".equals(result.get("status"))) {
                                    callback.onAgentError(agentName, (String) result.getOrDefault("error", "unknown"));
                                } else {
                                    callback.onAgentDone(agentName, result);
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
                                System.out.println("[Coordinator] Tracked " + tokens + " tokens from " + requiredAgents.get(i));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[Coordinator] " + requiredAgents.get(i) + " error: " + e.getMessage());
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

        private List<String> analyzeRequest(String request) {
            List<String> agents = new ArrayList<>();
            String lower = request.toLowerCase();

            if (lower.contains("backend") || lower.contains("java") || lower.contains("database") || 
                lower.contains("api") || lower.contains("spring") || lower.contains("cache")) {
                agents.add("backend");
            }

            if (lower.contains("frontend") || lower.contains("react") || lower.contains("ui") || 
                lower.contains("component") || lower.contains("typescript")) {
                agents.add("frontend");
            }

            if (lower.contains("test") || lower.contains("debug") || lower.contains("fix") || 
                lower.contains("review")) {
                agents.add("qa");
            }

            if (lower.contains("monitor") || lower.contains("metrics") || lower.contains("health")) {
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
            String userRequest = (String) payload.get("request");

            Map<String, Object> result = new HashMap<>(llmBridge.queryLLM("Backend optimization expert", userRequest, 256));
            this.lastExecutedTime = System.currentTimeMillis() - start;
            result.put("agent", "backend");
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
            String userRequest = (String) payload.get("request");

            Map<String, Object> result = new HashMap<>(llmBridge.queryLLM("Frontend React expert", userRequest, 256));
            this.lastExecutedTime = System.currentTimeMillis() - start;
            result.put("agent", "frontend");
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
            String userRequest = (String) payload.get("request");

            Map<String, Object> result = new HashMap<>(llmBridge.queryLLM("DevOps monitoring expert", userRequest, 256));
            this.lastExecutedTime = System.currentTimeMillis() - start;
            result.put("agent", "devops");
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
            String userRequest = (String) payload.get("request");

            Map<String, Object> result = new HashMap<>(llmBridge.queryLLM("QA testing expert", userRequest, 256));
            this.lastExecutedTime = System.currentTimeMillis() - start;
            result.put("agent", "qa");
            return result;
        }
    }

    public static class AgentMessage {
        public String targetAgent;
        public String type;
        public Object payload;
        public String replyTo;
        public long timestamp = System.currentTimeMillis();

        public AgentMessage(String targetAgent, String type, Object payload) {
            this.targetAgent = targetAgent;
            this.type = type;
            this.payload = payload;
        }

        public AgentMessage(String targetAgent, String type, Object payload, String replyTo) {
            this.targetAgent = targetAgent;
            this.type = type;
            this.payload = payload;
            this.replyTo = replyTo;
        }
    }
}
