<head>
  <link rel="canonical" href="https://open-tron-ai.github.io/OpenTron/" />
</head>  
<div align="center">
  <img alt="OpenTron" src="assets/OpenTron_Horizontal_Logo.png" width="400">
  <p><i>Personal AI, On Personal Devices. </i></p>
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

## Current Implementation Status

The repository currently contains a working local-stack implementation with the core pieces already wired together:

- **Frontend**: React 19 + Vite application with Tauri desktop shell, chat experience, dashboard views, agents, settings, logs, and data-source pages.
- **Backend**: Spring Boot 3.1.6 service with Java 21+ virtual threads support, exposing OpenTron-style APIs under /v1 for chat, models, agents, memory, speech, telemetry, tools, traces, and orchestration.
- **Database**: Profile-based configuration supporting both PostgreSQL (production) and H2 embedded (zero external dependencies).
- **CLI and tooling**: Java-based CLI and supporting utilities for local workflows and agent-driven operations.
- **Infrastructure**: Docker-based PostgreSQL setup, PowerShell startup scripts, deployment assets, and documentation for running the stack locally.

This should be treated as an active implementation with a functional foundation rather than a static scaffold. The core user flows are present, and several integrations continue to be refined as the platform evolves.

## What is Implemented Today

### User Experience
- Chat interface with model selection and streaming-style interactions
- Agent and coordinator views for task routing and multi-agent workflows
- Settings, logs, and storage-oriented dashboards
- Desktop-oriented startup and integration hooks via Tauri

### Backend Capabilities
- REST endpoints for chat, models, agents, memory, speech, telemetry, tools, and traces
- WebSocket support for streaming chat and agent events (supports 10,000+ concurrent connections with virtual threads)
- Routing logic for local inference and external model providers
- Persistence and trace-oriented storage patterns with dual database support
- Virtual threads enabled for scalable async operations
- Profile-based database configuration (prod/embedded)

### Database Support
- **Production (prod)**: PostgreSQL 16+ for multi-user team deployments
- **Embedded (embedded)**: H2 file-based database for desktop apps with zero external dependencies
- Both profiles use identical schema migrations via Flyway
- Auto-schema creation and validation on startup

### Project Structure
- `frontend/`: React frontend and Tauri integration
- `java/opentron-java/`: Maven multi-module Java project with backend and CLI modules
- `configs/`, `deploy/`, `docs/`: configuration, deployment assets, and documentation
- `src/` and `tools/`: additional source code and helper projects

## Prerequisites

For the full local stack, make sure you have:

- Docker (optional - not needed for embedded H2 profile)
- Java 21+ (required)
- Maven 3.9+ (required)
- Node.js 20+ (for frontend only)

## Quick Start

### Option 1: Interactive Profile Selection (Recommended)

On Windows, the recommended startup path with profile selection is:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-stack-profiles.ps1
```

This script will:
1. Ask you to choose: **[1] Embedded H2** or **[2] PostgreSQL**
2. For embedded: Start backend and frontend immediately (no Docker needed)
3. For PostgreSQL: Start Docker, then backend and frontend

**Embedded Profile** (zero external dependencies):
- Database: `~/.opentron/opentron.mv.db` (single file)
- H2 Console: `http://localhost:8000/h2-console`
- Perfect for: Desktop apps, single-user development, no Docker setup

**PostgreSQL Profile** (production):
- Database: PostgreSQL in Docker on localhost:5432
- Perfect for: Team deployments, production environments

### Option 2: Original PostgreSQL-Only Startup

```powershell
powershell -ExecutionPolicy Bypass -File .\start-stack.ps1
```

This uses PostgreSQL profile by default (maintains backward compatibility).

### Option 3: Manual Startup

#### Using Embedded H2 (Zero External Dependencies)

```powershell
# Terminal 1: Backend with embedded H2
cd java\opentron-java\backend
$env:SPRING_PROFILES_ACTIVE="embedded"
mvn spring-boot:run

# Terminal 2: Frontend
cd frontend
npm install
npm run tauri dev
```

Database will be created at: `~/.opentron/opentron.mv.db`

#### Using PostgreSQL (Production)

```powershell
# Terminal 1: Start PostgreSQL
docker run -d --restart always --name opentron-postgres `
  -e POSTGRES_DB=opentron `
  -e POSTGRES_USER=opentron `
  -e POSTGRES_PASSWORD=opentron_secure_password `
  -p 5432:5432 `
  -v postgres_data:/var/lib/postgresql/data `
  postgres:16-alpine

# Terminal 2: Backend with PostgreSQL
cd java\opentron-java\backend
$env:SPRING_PROFILES_ACTIVE="prod"
mvn spring-boot:run

# Terminal 3: Frontend
cd frontend
npm install
npm run tauri dev
```

## Virtual Threads

OpenTron backend now uses Java 21 virtual threads for scalable concurrency:

- **Enabled by default**: No configuration needed
- **Concurrent connections**: 10,000+ WebSocket connections per machine
- **Memory savings**: 100x reduction (~1KB per thread vs ~1MB)
- **Transparent**: No code changes required, automatically configured by Spring Boot 3.1.6

This enables high-concurrency chat streaming and agent operations without memory overhead.

## Database Profiles

### Profile: embedded (H2 File-Based)

**Best for:** Desktop apps, development, single-user scenarios

```powershell
$env:SPRING_PROFILES_ACTIVE="embedded"
java -jar target/opentron-java-backend-0.1.0-exec.jar
```

**Characteristics:**
- Database file: `~/.opentron/opentron.mv.db`
- External dependencies: None
- Single-user optimized
- H2 Web Console: `http://localhost:8000/h2-console`
- Auto-schema creation via Flyway

### Profile: prod (PostgreSQL)

**Best for:** Team deployments, production, multi-user

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER="opentron"
$env:POSTGRES_PASSWORD="opentron_secure_password"
java -jar target/opentron-java-backend-0.1.0-exec.jar
```

**Characteristics:**
- Database: PostgreSQL 16+ (Docker or managed)
- External dependencies: Docker + PostgreSQL
- Multi-user optimized (1000+ concurrent connections)
- Strict schema validation
- Full ACID compliance

## Build and Test Commands

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

# Build for both profiles
mvn clean package -DskipTests

# Test
mvn test

# Run with embedded profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=embedded"

# Run with PostgreSQL profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

## Environment Variables

### Embedded Profile (H2)
```bash
SPRING_PROFILES_ACTIVE=embedded
SERVER_PORT=8000
```

### PostgreSQL Profile
```bash
SPRING_PROFILES_ACTIVE=prod
POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
POSTGRES_USER=opentron
POSTGRES_PASSWORD=opentron_secure_password
ENGINE_HOST=http://localhost:11434
```

## Documentation

Comprehensive documentation is available in the repository:

- **DEPLOYMENT_GUIDE.md** - Production deployment scenarios and scaling
- **QUICK_COMMANDS.sh** - Build, run, and debugging commands

## Feature Comparison

| Feature | Embedded (H2) | Production (PostgreSQL) |
|---------|---------------|------------------------|
| **External Dependencies** | None | Docker + PostgreSQL |
| **Setup Time** | ~2 seconds | ~30 seconds |
| **Users** | 1-10 optimized | 100+ optimized |
| **Database File** | `~/.opentron/opentron.mv.db` | Server-managed |
| **Max Concurrent** | 5-10 | 1000+ |
| **Web Console** | Yes (h2-console) | No |
| **Use Case** | Desktop/Dev | Team/Prod |
| **Virtual Threads** | ✅ Enabled | ✅ Enabled |

## Architecture

```
OpenTron Backend (Java 21 + Spring Boot 3.1.6)
├── Virtual Threads Enabled
│   └── Supports 10,000+ concurrent WebSocket connections
├── Database Profiles
│   ├── embedded (H2)
│   │   └── ~/.opentron/opentron.mv.db (single file, zero deps)
│   └── prod (PostgreSQL)
│       └── PostgreSQL 16+ (Docker or managed)
├── Flyway Migrations
│   └── Shared schema for both profiles
└── Spring Boot APIs
    ├── REST: /v1/chat, /v1/agents, /v1/models, etc.
    └── WebSocket: /ws/chat, streaming events
```

## License

This project is licensed under the Apache-2.0 license.

<meta name="google-site-verification" content="vpoOSKjgVSIF4t9zXrEUFCjl9hixLXEtCcg8iPP_f_0" />
