<div align="center">
  <img alt="OpenTron" src="assets/OpenTron_Horizontal_Logo.png" width="400">
  <p><i>Personal AI, On Personal Devices.</i></p>
</div>

---

OpenTron: Autonomous Multi-Agent Orchestration

​OpenTron is a hardened, production-ready multi-agent ecosystem designed for local-first, autonomous engineering workflows. Unlike simple LLM wrappers, OpenTron utilizes a rigorous state-machine architecture to manage complex task decomposition, agent communication, and system-wide resilience.

​Core Architectural Pillars

​Contract-Driven Communication: All agent interactions are governed by strict schema-based protocols (JSON/Record-based), eliminating ambiguity and reducing hallucination.

​Hardened Agent Boundaries: Isolated execution workspaces ensure that agent-driven actions are sandboxed, preventing cascading failures and protecting the codebase.

​Autonomous Reliability: Built-in circuit breakers and automated retry logic manage agent timeouts and loop errors, ensuring the system remains stable without human intervention.

​Centralized Observability: Every task is logged within a unified audit trail, providing full transparency into the decision-making process of the agents.

​Continuous Gatekeeping: Integrated automated regression testing acts as the final gatekeeper, ensuring that any code generated or refactored by the agents meets production standards before commit.
​Why OpenTron?

​Most multi-agent systems are designed for prototyping. OpenTron is designed for operation. It bridges the gap between an AI assistant and an autonomous engineering team by providing the guardrails necessary to manage complex, multi-step engineering tasks in a "local-first" environment.

# OpenTron

OpenTron is a desktop-first AI platform for local-first experimentation and assisted engineering. The current repository combines a React/Vite/Tauri frontend, a Java/Spring Boot backend, a CLI module, and supporting infrastructure for PostgreSQL, Docker, and deployment.

## Current implementation status

The repository currently contains a working local-stack implementation with the core pieces already wired together:

- Frontend: a React 19 + Vite application with a Tauri desktop shell, chat experience, dashboard views, agents, settings, logs, and data-source pages.
- Backend: a Spring Boot service exposing OpenTron-style APIs under /v1 for chat, models, agents, memory, speech, telemetry, tools, traces, and orchestration.
- CLI and tooling: Java-based CLI and supporting utilities for local workflows and agent-driven operations.
- Infrastructure: Docker-based PostgreSQL setup, PowerShell startup scripts, deployment assets, and documentation for running the stack locally.

This should be treated as an active implementation with a functional foundation rather than a static scaffold. The core user flows are present, and several integrations continue to be refined as the platform evolves.

## What is implemented today

### User experience
- Chat interface with model selection and streaming-style interactions
- Agent and coordinator views for task routing and multi-agent workflows
- Settings, logs, and storage-oriented dashboards
- Desktop-oriented startup and integration hooks via Tauri

### Backend capabilities
- REST endpoints for chat, models, agents, memory, speech, telemetry, tools, and traces
- WebSocket support for streaming chat and agent events
- Routing logic for local inference and external model providers
- Persistence and trace-oriented storage patterns with PostgreSQL support

### Project structure
- frontend/: React frontend and Tauri integration
- java/opentron-java/: Maven multi-module Java project with backend and CLI modules
- configs/, deploy/, docs/: configuration, deployment assets, and documentation
- src/ and tools/: additional source code and helper projects

## Prerequisites

For the full local stack, make sure you have:

- Docker
- Java 21+
- Maven 3.9+
- Node.js 20+

## Quick start

On Windows, the recommended startup path is:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-stack.ps1
```

That script will:

1. Start PostgreSQL in Docker
2. Build the Java backend
3. Launch the backend in a separate terminal
4. Launch the frontend/Tauri app in a separate terminal

## Manual startup

### 1. Start PostgreSQL

```powershell
docker run -d --restart always --name opentron-postgres `
  -e POSTGRES_DB=opentron `
  -e POSTGRES_USER=opentron `
  -e POSTGRES_PASSWORD=opentron_secure_password `
  -p 5432:5432 `
  -v postgres_data:/var/lib/postgresql/data `
  postgres:16-alpine
```

### 2. Start the backend

```powershell
cd java\opentron-java\backend
mvn spring-boot:run
```

The backend is expected to run on http://localhost:8000.

### 3. Start the frontend

```powershell
cd frontend
npm install
npm run tauri dev
```

If you want to preview the web build instead of the desktop app, use:

```powershell
npm run dev
```

## Build and test commands

### Frontend

```powershell
cd frontend
npm run build
npm run build:tauri
npm test
```

### Backend

```powershell
cd java\opentron-java
mvn clean package -DskipTests
mvn test
```

## Environment variables

The stack expects a few environment variables for local development, including:

- POSTGRES_URL
- POSTGRES_USER
- POSTGRES_PASSWORD
- ENGINE_HOST

The default engine host is typically set to http://localhost:11434.

## License

This project is licensed under the Apache-2.0 license.

<meta name="google-site-verification" content="vpoOSKjgVSIF4t9zXrEUFCjl9hixLXEtCcg8iPP_f_0" />
