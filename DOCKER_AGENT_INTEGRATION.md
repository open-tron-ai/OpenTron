# Docker Agent Integration for Tron

## Changes Made

Add Docker Agent as an optional lightweight chat layer that runs in parallel with Ollama and the Java backend.

### File: `frontend/src-tauri/src/lib.rs`

**Line 11** - Add Docker Agent port constant:
```rust
const DOCKER_AGENT_PORT: u16 = 8001;  // Docker Agent chat overlay
```

**Line 345** - Add to BackendManager struct:
```rust
docker_agent: Option<ChildHandle>,
```

**Line 357** - Add to stop_all() method:
```rust
if let Some(ref mut h) = self.docker_agent {
    h.kill().await;
}
self.docker_agent = None;
```

**After line 1088** - Add Docker Agent startup phase (before "Phase 3: Start tron serve / Tron serve"):

```rust
    // Phase 3: Start Docker Agent (lightweight chat layer)
    // This runs in parallel and provides `/v1/chat/completions` on port 8001,
    // bypassing the Java backend's timeout issues if needed.
    {
        let mut s = status.lock().await;
        s.phase = "agent".into();
        s.detail = "Starting Docker Agent (optional chat layer)...".into();
    }

    let docker_bin = resolve_bin("docker");
    if std::path::Path::new(&docker_bin).exists() {
        // Try to create and run docker-agent container
        // This is optional; failure doesn't block the main server.
        let agent_config = "agents:\n  root:\n    model: ollama/mistral\n    description: Chat Assistant\n    instruction: |\n      You are a helpful chat assistant. Respond concisely and conversationally.\n      Answer user questions directly.\n    toolsets:\n      - type: shell\n      - type: think\n";

        let config_dir = std::path::PathBuf::from(home_dir())
            .join(".OpenTron")
            .join("docker-agent");
        let _ = std::fs::create_dir_all(&config_dir);
        let config_path = config_dir.join("agent.yaml");
        if let Ok(_) = std::fs::write(&config_path, agent_config) {
            let mut agent_cmd = tokio::process::Command::new(&docker_bin);
            agent_cmd
                .args(&["run", "--rm", "--net", "host"])
                .arg("-v").arg(format!("{}:/agent", config_dir))
                .arg("docker-agent:latest")
                .arg("serve")
                .arg("--port").arg(DOCKER_AGENT_PORT.to_string())
                .stdout(std::process::Stdio::null())
                .stderr(std::process::Stdio::null());
            prepare_subprocess_for_appimage(&mut agent_cmd);
            if let Ok(child) = agent_cmd.spawn() {
                backend.lock().await.docker_agent = Some(ChildHandle { child });
            }
        }
    }

    // Phase 4: Start tron serve / Tron serve (relabel as Phase 4)
```

## Why This Works

1. **Runs during app launch** - Docker Agent starts automatically when Tron launches, alongside Ollama
2. **Non-blocking failure** - If Docker isn't installed or the image isn't available, the main Java backend still starts normally
3. **Direct Ollama connection** - Docker Agent talks directly to Ollama (localhost:11434), no HTTP API deadlock issues
4. **Separate port** - Runs on port 8001, doesn't interfere with Java backend on 8000
5. **Lightweight** - Mistral 7B model is fast and fits in constrained RAM
6. **Persistent storage** - Agent config stored in ~/.OpenTron/docker-agent/agent.yaml for future reuse

## Usage After Integration

Once rebuilt, the desktop app will:
1. Start Ollama on port 11434
2. **Attempt to start Docker Agent on port 8001** (optional, non-blocking)
3. Start Java backend on port 8000

Frontend can then offer chat via either endpoint:
- Port 8001 (Docker Agent - reliable, streaming-friendly)
- Port 8000 (Java backend - full feature set)

## Installation Requirements

Users need Docker installed and the Docker Agent image available:
```bash
docker pull docker-agent:latest
# or build locally if not published yet
```

If Docker is unavailable, Tron still works - Docker Agent simply won't start.

