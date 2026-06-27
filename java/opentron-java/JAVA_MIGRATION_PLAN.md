Plan: Java Migration Implementation
TL;DR: Stabilize the Java backend and desktop launch path first, then convert the CLI entrypoint and command wrappers, then migrate core runtime packages and remaining stubbed Java modules. The plan is ordered so the desktop app can boot the Java backend and the CLI can run natively before the rest of the application is fully converted.

Steps

Baseline validation

Build opentron-java with mvn clean package -DskipTests and confirm current compilation.
Run backend integration test EngineStubIntegrationTest.java to capture current proxy behavior.
Confirm lib.rs currently prefers Java backend only if TRON_JAVA_JAR / compiled classes are present and otherwise falls back to Python uv run tron serve.
Implement a production Java serve backend launch path

Replace/stabilize Serve.java so it launches the Spring Boot backend or equivalent Java server rather than a minimal stub.
Add a Java-side /health endpoint for http://127.0.0.1:8000/health.
Ensure OpentronBackendApplication.java supports configured host/port and engine routing.
Validate backend startup configuration in application.properties.
Wire desktop boot to the Java backend path

Update lib.rs to prefer build_java_tron_command for tron serve boot.
Improve find_java_backend_root() and find_java_backend() detection for JAR and classpath cases.
Confirm run_Tron_command() path uses Java when available and falls back cleanly to uv only when not.
Document runtime env vars: TRON_JAVA_JAR, OPENTRON_JAVA_ROOT, TRON_JAVA_ROOT.
Convert the CLI entrypoint from Python delegation to Java-native dispatch

Update Main.java and Serve.java so serve runs Java backend and other commands dispatch to Java implementations when ready.
Migrate high-priority CLI commands required by desktop and core workflows, e.g. Ask.java, ChannelsCmd.java, ConfigCmd.java, DaemonCmd.java, MemoryCmd.java, Model.java, ToolCmd.java.
Keep lower-priority commands as Python delegators until their Java replacements exist.
Port high-value Java runtime packages needed by backend and desktop

Implement java/opentron-java/src/main/java/io/opentron/workflow/{Loader,Graph,Engine,Builder,Types}.java.
Implement LearningOrchestrator.java and io/opentron/server/ChannelBridge.java.
Implement java/opentron-java/src/main/java/io/opentron/traces/{Store,Collector,Analyzer}.java and io/opentron/telemetry/Wrapper.java.
Implement high-impact tools: java/opentron-java/src/main/java/io/opentron/tools/{WebSearch,Think,TextToSpeech,StorageTools}.java.
Convert memory and agents packages that backend controllers rely on.
Gradually remove Python delegation

Replace Utils.runPythonCli() in top-level wrappers once Java replacements are functional.
Preserve fallback helpers only for legacy migration scenarios.
Update README.md and BUILD_INSTRUCTIONS.md to document Java-native runtime.
Expand backend functionality beyond proxy mode

Implement full service-backed controllers for memory, agents, traces, and any config/agent endpoints.
Ensure WebSocket relay /v1/chat/stream and SSE proxy support are production-ready.
Add backend tests for /health, /v1/models, /v1/chat/completions, /v1/chat/stream, /v1/memory/search, and /v1/agents.
Quality and verification

Add JUnit tests for CLI dispatch and Java backend startup.
Add desktop boot tests for build_java_tron_command() if possible.
Track remaining stubs by searching for UnsupportedOperationException("Auto-generated stub").
Relevant files

Main.java
Serve.java
Utils.java
OpentronBackendApplication.java
ChatController.java
ModelsController.java
ForwardingController.java
ReactiveChatWebSocketHandler.java
WebSocketConfig.java
lib.rs
EngineStubIntegrationTest.java
Loader.java
Graph.java
Engine.java
Builder.java
LearningOrchestrator.java
ChannelBridge.java
Verification

Build the Java project: cd java/opentron-java && mvn clean package -DskipTests.
Run mvn -f pom.xml test for backend test coverage.
Start the desktop app and verify it attaches to the Java backend and passes health checks.
Execute converted CLI commands natively and confirm they run without Python delegation.
Search for remaining stubs with grep -R "Auto-generated stub" java/opentron-java/src/main/java.
Decisions

Prioritize Java backend and desktop launch before full CLI and runtime migration.
Keep Python delegation only as a temporary fallback path.
Use the existing backend module as the production Java server; Serve.java should delegate to it.
Further considerations

If release packaging must include the Java backend, add a dedicated fat JAR and desktop packaging step.
Consider whether backend should be merged into the main opentron-java module once the server is stable.
Clarify if the final migration goal includes retiring OpenTron entirely or supporting a compatibility mode.