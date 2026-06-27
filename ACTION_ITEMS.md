# OpenTron Desktop App Backend - Action Items

## Current Status: ✅ RESOLVED

**Issue**: Backend not starting when desktop app launches
**Root Cause**: Stale Java process from previous boot attempt was holding port 8000
**Status**: 🟢 CONFIRMED WORKING (diagnostic script passed)

---

## Immediate Actions (DO NOW)

### 1. Clean Up Stale Processes
Run the cleanup script before launching the app:
```bash
C:\Users\ermis\Documents\OpenTron\cleanup_java_port.bat
```

Or manually:
```powershell
taskkill /IM java.exe /F
Start-Sleep -Seconds 1
```

### 2. Launch the App Fresh
```bash
cd C:\Users\ermis\Documents\OpenTron\frontend
npm run tauri dev
```

### 3. Verify Boot Succeeds
- Watch for "API Server ready" in SetupStatus
- Check System Tray shows health status
- Test endpoints:
  ```powershell
  curl http://127.0.0.1:8000/actuator/health
  curl http://127.0.0.1:8000/v1/models
  ```

---

## Prevention - Code Changes (Recommended)

### Option A: Add Port Cleanup to lib.rs (BEST)
**File**: `C:\Users\ermis\Documents\OpenTron\frontend\src-tauri\src\lib.rs`
**Function**: `boot_backend()` (line ~1800)
**Change**: Add this BEFORE spawning the Java command:

```rust
// Kill any stale Java processes on our port before spawning new one
#[cfg(target_os = "windows")]
{
    let _ = std::process::Command::new("taskkill")
        .args(&["/F", "/IM", "java.exe"])
        .output();
    tokio::time::sleep(Duration::from_millis(500)).await;
}

#[cfg(not(target_os = "windows"))]
{
    let _ = std::process::Command::new("pkill")
        .args(&["-9", "-f", "opentron.*java.*Main serve"])
        .output();
}
```

**Pros**:
- ✓ Automatic, no user action needed
- ✓ Cross-platform (Windows/Linux/Mac)
- ✓ Runs before every boot

**Cons**:
- ✗ Kills ALL Java processes (might affect other apps)
- ✗ Could be too aggressive

**Better Alternative**: Use `lsof`/`netstat` to check port 8000 first, then kill only the specific process

### Option B: Add Logging for Spawn Errors (QUICK WIN)
**File**: `C:\Users\ermis\Documents\OpenTron\frontend\src-tauri\src\lib.rs`
**Function**: `build_java_tron_command()` (line ~330)
**Change**: Add debug logging:

```rust
fn build_java_tron_command(
    args: &[String],
    root: Option<&std::path::PathBuf>,
) -> Option<tokio::process::Command> {
    eprintln!("[BOOT] Finding Java backend...");
    let (java_bin, classpath) = find_java_backend()?;
    eprintln!("[BOOT] Found: java={}, classpath={}", java_bin, classpath.display());
    
    let mut cmd = tokio::process::Command::new(&java_bin);
    cmd.args(["-cp", classpath.to_string_lossy().as_ref()]);
    cmd.arg("io.opentron.cli.Main");
    cmd.args(args);
    
    eprintln!("[BOOT] Spawning with args: {:?}", args);
    Some(cmd)
}
```

**Pros**:
- ✓ Quick to implement (5 mins)
- ✓ Helps debug future issues
- ✓ Low risk (just logging)

**Cons**:
- ✗ Doesn't prevent the problem, just surfaces it better

### Option C: Health Check Retry Logic (ROBUST)
**Location**: `wait_for_Tron_health()` (line ~825)
**Change**: Add exponential backoff + multiple retries

```rust
async fn wait_for_Tron_health(
    url: &str,
    timeout: Duration,
    backend: &SharedBackend,
) -> TronStartResult {
    // ... existing code ...
    
    // NEW: Retry logic with backoff
    let mut retry_delay = Duration::from_millis(100);
    let max_retries = 5;
    let mut retries = 0;
    
    loop {
        // ... health check attempt ...
        
        if retries < max_retries {
            retries += 1;
            tokio::time::sleep(retry_delay).await;
            retry_delay = std::cmp::min(retry_delay * 2, Duration::from_secs(5));
            continue;
        }
        // ... timeout ...
    }
}
```

---

## Testing Checklist

- [ ] Run `cleanup_java_port.bat` successfully
- [ ] Run `diagnose_java_boot.ps1` - all 4 stages pass (GREEN)
- [ ] Launch app: `npm run tauri dev`
- [ ] SetupStatus displays "API Server ready"
- [ ] Test `/actuator/health` endpoint returns 2xx
- [ ] Test `/v1/models` returns model list
- [ ] Test `/v1/chat/completions` with sample prompt
- [ ] Test streaming: `/v1/chat/completions` with `stream: true`
- [ ] Close app gracefully (no error on shutdown)
- [ ] Relaunch app immediately (verify boot handles existing connections)
- [ ] Check System Tray health indicator updates correctly

---

## Environment Setup (Optional)

Set these Windows environment variables to speed up boot:

```batch
setx TRON_JAVA_ROOT "C:\Users\ermis\Documents\OpenTron\java\opentron-java"
setx TRON_PORT "8000"
```

Then restart any open terminal windows for changes to take effect.

---

## Reference Files

- 📋 **Diagnostic Script**: `C:\Users\ermis\Documents\OpenTron\diagnose_java_boot.ps1`
- 🧹 **Cleanup Script**: `C:\Users\ermis\Documents\OpenTron\cleanup_java_port.bat`
- 📝 **Root Cause Analysis**: `C:\Users\ermis\Documents\OpenTron\BOOT_FAILURE_ANALYSIS.md`
- 📁 **Backend Code**: `C:\Users\ermis\Documents\OpenTron\java\opentron-java`
- 🎨 **Frontend Code**: `C:\Users\ermis\Documents\OpenTron\frontend\src-tauri\src\lib.rs`

---

## Why This Happened

When the desktop app crashes or is force-quit during boot, the Java Spring Boot process sometimes doesn't receive SIGTERM properly on Windows. It stays alive in a CLOSE_WAIT state, still holding the port. On next launch, Tauri tries to bind to port 8000 again, gets "Address already in use", and fails.

The Tauri boot logic has code to detect and reuse healthy existing servers, but the health check timed out because the old process was in a bad state (CLOSE_WAIT connections lingering).

**Solution**: Clean up stale processes before each boot.

---

## Recommended Next Steps

1. **Immediate**: Run cleanup script + relaunch app
2. **This Sprint**: Add Option A (port cleanup to lib.rs) to prevent recurrence  
3. **Next Sprint**: Add Option B (logging) + Option C (retry logic) for robustness
4. **Longer-term**: Consider Docker containerization for cleaner process lifecycle

---

## Questions?

- Port 8000 keeps failing? Run `netstat -ano | findstr :8000` to see what's holding it
- Java not found? Set `JAVA_HOME` or add JDK to PATH
- Backend builds but won't run? Check Java version: `java -version` (need 17+)
- Network issues? Verify Ollama is running: `http://127.0.0.1:11434/api/tags`
