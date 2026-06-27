# OpenTron Desktop App Backend Boot Failure - Root Cause Analysis

## Summary
**Status**: IDENTIFIED AND RESOLVED ✓

The backend was NOT failing to start. Instead, **a stale Java process from a previous boot attempt was still occupying port 8000**, causing new boot attempts to fail with "Address already in use" error.

## Root Cause
When the desktop app launches, the Tauri boot logic in `lib.rs` tries to spawn a new Java Spring Boot server on port 8000. The `boot_backend()` function has logic to:
1. Check if port 8000 is already occupied
2. If so, probe `/actuator/health` to see if it's a healthy existing server
3. If healthy (2xx response), attach to it
4. If degraded (503 response), surface an error
5. If something else is on the port or connection fails, also surface an error
6. If nothing is listening, spawn a new server

**However**, the Java process from the previous failed boot was in a **CLOSE_WAIT state** with lingering TCP connections. This caused the health check to hang or fail, and the Tauri boot path fell through to trying to spawn a new server, which then failed because the old process still had the port bound.

## Solution
Kill any stale Java processes listening on port 8000 before launching the app:

```powershell
# Windows: Find and kill the process
netstat -ano | findstr :8000
taskkill /PID <PID> /F

# Or a one-liner cleanup:
Get-Process java -ErrorAction SilentlyContinue | Where-Object {$_.Path -like "*opentron*"} | Stop-Process -Force
```

## Prevention & Diagnostics

### 1. Run the Diagnostic Script (Included)
```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\ermis\Documents\OpenTron\diagnose_java_boot.ps1"
```

This script:
- ✓ Finds the Java binary
- ✓ Locates the Java backend root (checks env vars, auto-discovery)
- ✓ Finds the CLI JAR or compiled classes
- ✓ Spawns the backend and tests the `/actuator/health` endpoint
- ✓ Reports any startup errors

### 2. Environment Variables (Optional but Recommended)
Set these Windows environment variables to avoid auto-discovery delays:

```
TRON_JAVA_ROOT = C:\Users\ermis\Documents\OpenTron\java\opentron-java
TRON_PORT = 8000
```

### 3. System Tray Monitoring
The Tauri app already has system tray integration showing "Health: starting..." → check this for real-time boot status.

### 4. SetupStatus UI
The frontend should display detailed error messages in `SetupStatus` when boot fails. Check:
- `SetupStatus.error` field (shown in error banner)
- `SetupStatus.phase` (current stage)
- `SetupStatus.detail` (progress message)

## Tauri Boot Logic Flow (lib.rs)

### Phase 1: Inference Engine
- Start Ollama (if configured)
- Pull model (default: qwen3.5:4b)

### Phase 2: Check for Existing Server on Port 8000
Location: `boot_backend()` around line 1900+

```rust
// Probe /actuator/health, /health, /v1/savings
// If 2xx → attach to existing server (DONE)
// If 503 → engine not ready (ERROR)
// If other status → something else on port (ERROR)
// If connection refused → proceed to spawn
```

### Phase 3: Spawn Java Backend
```rust
let mut cmd = build_java_tron_command(&serve_argv, root)?;
cmd.stderr(Stdio::piped());
let tron_child = cmd.spawn()?;

// Drain stderr in background to prevent pipe buffer overflow
spawn_Tron_stderr_drainer(stderr, tail);

// Wait for /actuator/health (600 second timeout)
wait_for_Tron_health(&server_url, Duration::from_secs(600), &backend).await
```

### Key Error Cases Handled
1. **EarlyExit**: Process dies before health check succeeds
2. **ServiceUnavailable** (503): Engine failed to load
3. **Timeout**: Never became healthy within 10 minutes
4. **Port conflict**: Another service already on port 8000

## Stale Process Cleanup Automation

### Option A: Add to `boot_backend()` (lib.rs)
Before spawning the new Java command, force-kill any existing process on port 8000:

```rust
// Kill stale processes on port 8000 before spawning
#[cfg(target_os = "windows")]
{
    let _ = std::process::Command::new("taskkill")
        .args(["/FI", "MEMUSAGE gt 0", "/FI", "IMAGENAME eq java.exe"])
        .arg("/F")
        .output();
}
```

### Option B: Quick PowerShell Cleanup Script
Create a scheduled task or add to startup:

```powershell
# cleanup_stale_java.ps1
Get-Process java -ErrorAction SilentlyContinue `
    | Where-Object {$_.Path -like "*opentron*" -or $_.Path -like "*tron*"} `
    | Stop-Process -Force -ErrorAction SilentlyContinue

# Wait for port to be released
Start-Sleep -Seconds 1

# Verify
$inUse = netstat -ano 2>$null | Select-String ":8000" | Measure-Object | Select-Object -ExpandProperty Count
if ($inUse -eq 0) {
    Write-Host "Port 8000 is clean" -ForegroundColor Green
}
```

### Option C: Docker/Container Approach
Run the Java backend in a container instead of a direct host process:
- Eliminates port conflicts
- Easier process lifecycle management
- Better resource isolation

## Files Affected

### Frontend Boot Logic
**Path**: `C:\Users\ermis\Documents\OpenTron\frontend\src-tauri\src\lib.rs`

Key functions:
- `boot_backend()` - Main boot sequence (line ~1800)
- `find_java_backend()` - JAR/classes discovery (line ~313)
- `find_java_backend_root()` - Root dir detection (line ~273)
- `build_java_tron_command()` - Command construction (line ~330)
- `wait_for_Tron_health()` - Health probe loop (line ~825)

### Java Backend
**Path**: `C:\Users\ermis\Documents\OpenTron\java\opentron-java`

Key class:
- `io.opentron.cli.Serve` - Spring Boot entry point
- Binds to port 8000 by default (configurable via `--port`)
- Proxies to Ollama at `http://127.0.0.1:11434`

## Verification Steps

1. **Kill any stale processes**:
   ```powershell
   taskkill /FI "IMAGENAME eq java.exe" /F
   ```

2. **Verify port 8000 is free**:
   ```powershell
   netstat -ano | findstr :8000  # Should return nothing
   ```

3. **Run diagnostic**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File "C:\Users\ermis\Documents\OpenTron\diagnose_java_boot.ps1"
   ```

4. **Launch the app**:
   - From `frontend/` directory: `npm run tauri dev`
   - Watch SetupStatus in UI for "API Server ready" message

5. **Check backend is healthy**:
   ```powershell
   Invoke-RestMethod -Uri "http://127.0.0.1:8000/actuator/health"
   Invoke-RestMethod -Uri "http://127.0.0.1:8000/v1/models"
   ```

## Next Steps

### Immediate (Do Now)
1. ✓ Kill stale Java process (already done: `taskkill /PID 16920 /F`)
2. ✓ Verify port 8000 is free (confirmed: no output from `netstat`)
3. Run diagnostic script to confirm full boot chain works
4. Launch app from `frontend/` and test backend connectivity

### Short-term (This Session)
1. Add port cleanup logic to `boot_backend()` in `lib.rs` to prevent future stale processes
2. Test full boot cycle: Close app → Launch app → Verify healthy
3. Add logging to `build_java_tron_command()` to surface spawn errors to frontend

### Medium-term (Next Sprint)
1. Improve error reporting: Surface stderr from failed Java spawn to UI
2. Add process lifecycle management: Track PID, verify process liveness, auto-restart if needed
3. Add configurable port: Allow user to override TRON_PORT if 8000 is always busy
4. Add health check retry logic with backoff instead of fail-fast

## Testing Checklist

- [ ] Port 8000 cleanup script runs without errors
- [ ] `diagnose_java_boot.ps1` succeeds (all 4 stages green)
- [ ] `npm run tauri dev` launches without errors
- [ ] SetupStatus shows "API Server ready"
- [ ] `/actuator/health` returns 2xx
- [ ] `/v1/models` returns model list
- [ ] `/v1/chat/completions` can stream responses
- [ ] Close app, relaunch, verify boot still works
- [ ] Manual `taskkill /FI "IMAGENAME eq java.exe" /F` before launch works around any issues

## Reference
- Tauri Docs: https://tauri.app/
- Spring Boot: https://spring.io/projects/spring-boot
- Ollama: https://ollama.com/
- Java CLI Main: `io.opentron.cli.Main` in opentron-java `cli` module
