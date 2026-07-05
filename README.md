<div align="center">
  <img alt="OpenTron" src="assets/OpenTron_Horizontal_Logo.png" width="400">
  <p><i>Personal AI, On Personal Devices.</i></p>
</div>

---

OpenTron
Personal AI, On Personal Devices.

# OpenTron

OpenTron is a desktop-first AI assistant platform that combines a React and Tauri frontend with a Java backend, a Python-based AI toolkit, and optional PostgreSQL-backed storage. The project is aimed at local-first experimentation with chat, agents, telemetry, connectors, and voice workflows.

## What this repository contains

- Frontend: a React 19 + Vite + Tauri app for chat, dashboards, agents, settings, and logs
- Backend: a Spring Boot service that exposes OpenTron-style API routes for chat, models, agents, memory, traces, speech, and telemetry
- Python package: modular AI primitives, CLI entry points, and supporting tooling under the source tree
- Infrastructure: Docker, deployment scripts, docs, and helper utilities for local development and testing

## Core capabilities

- Chat interface with model selection and streaming responses
- Multi-agent coordination and task orchestration
- Speech and voice-related endpoints and UI hooks
- Memory, trace, connector, and telemetry surfaces
- Local inference routing with optional cloud connectors
- PostgreSQL integration for persistent storage and dashboards

## Repository layout

- frontend/: React frontend and Tauri desktop integration
- java/opentron-java/: Java modules, including the backend and CLI
- src/: Python package and core implementation
- configs/, deploy/, docs/, scripts/: project configuration, deployment assets, and docs
- examples/, assets/, tests/: sample data, static assets, and test coverage

## Prerequisites

For the full stack locally, make sure you have:

- Docker
- Java 21+
- Maven 3.9+
- Node.js 20+
- Python 3.10–3.13 (for the Python tooling)

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

## Build commands

### Frontend

```powershell
cd frontend
npm run build
npm run build:tauri
```

### Backend

```powershell
cd java\opentron-java
mvn clean package -DskipTests
```

### Python tooling

If you are working on the Python-oriented pieces, the repository includes a Python project definition in pyproject.toml and an accompanying lock file.

## Environment notes

The stack expects a few environment variables for local development, including:

- POSTGRES_URL
- POSTGRES_USER
- POSTGRES_PASSWORD
- ENGINE_HOST

The default engine host is typically set to http://localhost:11434.

## Documentation

Useful references in this repository include:

- QUICK_START.md
- HOW_TO_START.md
- BUILD_INSTRUCTIONS.md
- START_HERE.md
- docs/
- various integration notes under the root for PostgreSQL, agents, speech, and deployment

## Status

This repository is actively evolving and includes both completed integrations and work-in-progress areas. Some features may be partially wired up depending on the backend and frontend endpoints currently available.

## License

This project is licensed under the Apache-2.0 license.
