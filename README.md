<div align="center">
  <img alt="OpenTron" src="assets/OpenTron_Horizontal_Logo.png" width="400">
  <p><i>Personal AI, On Personal Devices.</i></p>
</div>

---

OpenTron
Personal AI, On Personal Devices.

OpenTron is a hybrid AI assistant application combining a React/Tauri frontend with a Java Spring Boot backend. The backend proxies chat requests to local inference engines and cloud providers, while the frontend provides a desktop-style AI chat experience plus managed agent, tool, and telemetry interfaces.

Architecture
High-level structure
frontend

React 19 + Vite frontend
Tauri desktop integration
Local app state with Zustand
API clients under lib
UI pages and components under components
backend

Spring Boot backend
WebFlux HTTP client via WebClient
WebSocket support
Controllers under src/main/java/org/opentron/backend/controllers
Services and utilities under src/main/java/org/opentron/backend/util
Data flow
Frontend sends /v1/* requests to the local Java backend.
Backend routes chat and model requests to the configured inference engine host.
Backend may route requests to:
local Ollama inference
Hugging Face local/API
cloud models via CloudModelService
Backend returns JSON or SSE streams back to the frontend.
Frontend updates chat UI, agent status, telemetry, memory, traces, and settings.
Key components
Backend entrypoint: OpentronBackendApplication.java
Frontend entrypoint: main.tsx
Engine routing: EngineRouting.java
Chat controller: ChatController.java
Agent coordinator: AgentsController.java
WebSocket chat: ReactiveChatWebSocketHandler.java
Model listing: ModelsController.java
Behavior
Backend behavior
The Java backend implements a minimal OpenTron-compatible API:

/v1/models

Lists available models from the configured engine host.
/v1/chat/completions

Receives chat payloads.
Detects cloud models and routes them to cloud provider handlers.
Supports Hugging Face and Ollama backends.
Attempts to proxy engine responses to the frontend.
/v1/recommended-model

Returns a model recommendation.
/v1/info

Returns server info and diagnostics.
/v1/telemetry/*

Energy and stats endpoints.
/v1/memory/*

Memory store, search, and stats.
/v1/traces/*

Trace retrieval.
/v1/speech/*

Speech health, transcription, synthesis, and voice listing.
/v1/jarvis/*

Jarvis voice health and speak endpoints.
/v1/managed-agents/*

Demo managed agent CRUD and lifecycle actions.
/v1/agents/*

Multi-agent coordination and task polling.
/v1/connectors/*

Connector listing and connect/disconnect stubs.
/v1/tools/*

Tool listing and credential endpoints.
Backend infrastructure:

WebClient is configured with a connection pool and engine host.
CORS is enabled for local development and Tauri origins.
The backend can forward allowed /v1 routes to the engine host using EngineRouting.
A simple in-memory managed agent store exists for demo/testing.
Frontend behavior
The frontend:

Builds a chat UI with routes for chat, dashboard, agents, settings, and logs.
Fetches models, server info, savings, and telemetry on mount.
Uses local storage for settings, conversations, opt-in state, and model selection.
Supports Tauri-specific behavior when running as a desktop app.
Uses fetch / apiFetch wrappers to call the backend.
Supports SSE chat streaming and WebSocket chat streaming.
Includes features for:
model selection
opt-in analytics
saved conversations
speech input and voice output
connector management
agent coordination and task queueing
trace/debug exploration
How to use
Backend
Open a terminal in:

backend
Build:

mvn package
Run:

java -jar target/opentron-java-backend-0.1.0.jar
or if the repackaged Spring Boot jar is produced with a classifier:
java -jar target/opentron-java-backend-0.1.0-exec.jar
Configuration

Default engine host: http://localhost:11434
Override with:
-Dengine.host=http://your-engine:port
-Dengine.apiKey=YOUR_KEY
-Dengine.type=ollama|openai|auto
Frontend
Open a terminal in:

frontend
Install dependencies:

npm install
Run in development mode:

npm run dev
Build for production:

npm run build
Build for Tauri:

npm run build:tauri
Preview production build:

npm run preview
Run Tauri (desktop):

npm run tauri
Local development flow
Start the Java backend first so http://localhost:8000 is available.
Start the frontend dev server.
The frontend proxy configuration in vite.config.ts forwards /v1, /health, and /api to the backend.
Usage
Launch backend.
Start frontend or desktop app.
Open the app in browser or Tauri shell.
Use the chat page to send messages.
Visit dashboard and settings for telemetry, model selection, and integrations.
Use agents and connectors screens to explore multi-agent and external data features.
Limitations / known gaps
This repository currently contains a partial implementation:

The backend implements a subset of frontend API routes.
Some frontend paths are not yet implemented in the Java backend.
Because of these mismatches, not all frontend features will work end-to-end until the missing backend endpoints are implemented.

Project layout
frontend — React + Vite + Tauri UI
backend — Java Spring Boot backend
pom.xml — backend build config
package.json — frontend scripts and dependencies
vite.config.ts — frontend build/proxy configuration