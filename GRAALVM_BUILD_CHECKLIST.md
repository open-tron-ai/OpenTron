# GraalVM Native Backend Implementation - Build & Test Checklist

## Files Modified / Created

### New Files
- ✅ `frontend/src-tauri/src/native_backend.rs` — Native binary resolver module
- ✅ `GRAALVM_NATIVE_INTEGRATION.md` — Full implementation documentation

### Modified Files
- ✅ `java/opentron-java/backend/pom.xml` — Added GraalVM native-image plugin + platform profiles
- ✅ `frontend/src-tauri/src/lib.rs` — Native-first boot sequence, module import, updated error messages

## Verification Steps

### 1. POM XML Validation
```bash
cd java/opentron-java/backend
mvn help:describe -Dplugin=org.graalvm.buildtools:native-maven-plugin
# Should show: native-maven-plugin 0.10.2
```

### 2. Native Backend Build (Requires GraalVM JDK 21+)

**Prerequisites:**
- GraalVM JDK 21+ installed
- Set `JAVA_HOME` to GraalVM installation

**Build Command:**
```bash
cd java/opentron-java/backend

# Option A: Let Maven auto-detect platform
mvn -DskipTests=true clean package

# Option B: Explicit platform profile
mvn -DskipTests=true -Pnative-macos-aarch64 clean package
# Alternatives:
#   -Pnative-linux-x86_64
#   -Pnative-linux-aarch64
#   -Pnative-windows-x86_64
```

**Expected Output:**
```
[INFO] Building GraalVM native-image...
[INFO] Native image built in <time>
[INFO] BUILD SUCCESS
```

**Output Binary:**
- `backend/target/opentron-backend-<platform>` (e.g., `opentron-backend-macos-aarch64`)

### 3. Tauri Code Validation

```bash
cd frontend/src-tauri

# Check Rust code compiles
cargo check
# Should complete without errors

# Build release
cargo build --release
# Output: frontend/src-tauri/target/release/app.app (macOS) or similar
```

### 4. Runtime Behavior

**With Native Binary Present:**

1. Copy binary to app:
   ```bash
   cp java/opentron-java/backend/target/opentron-backend-* frontend/src-tauri/sidecar/
   ```

2. Launch app
3. Observe setup screen: "Starting native API server..." message
4. Backend should be ready in ~0.5-1.5 seconds
5. Check process: `ps aux | grep opentron-backend` (not `java`)

**With Java Fallback:**

1. Do NOT copy native binary
2. Launch app
3. Observe setup screen: "Starting API server..." message
4. Backend ready in ~3-5 seconds (JVM startup time)
5. Check process: `ps aux | grep java` or `jps -l`

**With Neither Available:**

1. Delete/hide both native binary and Java JAR
2. Launch app
3. Setup screen displays error with instructions
4. Verify clear guidance on both paths (native or Java)

### 5. Detailed Setup Screen Status

The setup screen (active during boot) should show:

| Scenario | Status Message | Time to Ready |
|----------|---|---|
| Native binary found | "Starting native API server..." | 0.5–1.5s |
| Java fallback | "Starting API server..." | 3–5s |
| Neither found | Error + instructions | N/A |

### 6. Configuration Overrides

Test environment variable precedence:

```bash
# Force Java backend (skip native):
export TRON_JAVA_ROOT=/path/to/opentron/java/opentron-java
# Launch app → should use Java

# Force specific JAR:
export TRON_JAVA_JAR=/path/to/custom-backend.jar
# Launch app → should use that JAR

# With GraalVM native sidecar:
# (binary in app bundle or src-tauri/sidecar/)
# Launch app → should prefer native
```

## Build Profiles Reference

| Profile | OS | Architecture | Output Binary |
|---------|-------|-------|-----------|
| `native-linux-x86_64` | Linux | x86_64 | `opentron-backend-linux-x86_64` |
| `native-linux-aarch64` | Linux | ARM64 | `opentron-backend-linux-aarch64` |
| `native-macos-x86_64` | macOS | x86_64 | `opentron-backend-macos-x86_64` |
| `native-macos-aarch64` | macOS | ARM64 (M1+) | `opentron-backend-macos-aarch64` |
| `native-windows-x86_64` | Windows | x86_64 | `opentron-backend-windows-x86_64.exe` |

## Troubleshooting

### Native Build Fails: "No such file or directory"
- **Cause:** GraalVM not installed or `JAVA_HOME` not set correctly
- **Fix:** `export JAVA_HOME=/path/to/graalvm && mvn ...`

### Native Binary Not Detected at Runtime
- **Check:** Binary is in app sidecar or `target/native-image/`
- **Check:** Binary name matches platform (e.g., `opentron-backend-macos-aarch64` on M1 Mac)
- **Fallback:** Java launcher will be used

### Tauri Build Fails: "module native_backend not found"
- **Cause:** `frontend/src-tauri/src/native_backend.rs` not created
- **Fix:** Verify file exists at `frontend/src-tauri/src/native_backend.rs` (2026 bytes)

### Setup Screen Hangs After "Starting..."
- **Cause:** Backend process failed to start or health check timed out
- **Fix:** Check logs: `journalctl -u docker.service` or app debug output
- **Fallback:** Close and relaunch; Java will retry

## Deployment Notes

### Desktop Bundle with Native Binary
```bash
# 1. Build native binary
cd java/opentron-java/backend
mvn -DskipTests=true clean package

# 2. Copy to app sidecar
mkdir -p frontend/src-tauri/sidecar
cp backend/target/opentron-backend-* frontend/src-tauri/sidecar/

# 3. Bundle desktop app
cd frontend/src-tauri
cargo build --release

# Result: App binary includes native backend; no Java runtime needed
```

### Desktop Bundle with Java Fallback (No Native Build)
```bash
# Skip native build, just build desktop app
cd frontend/src-tauri
cargo build --release

# Result: App includes Java fallback; users need Java 21+ installed
```

## Next Steps

1. **Test native build** on target platform with GraalVM installed
2. **Verify setup screen** displays correct status during startup
3. **Measure startup time** native vs. Java (should see 4-8s improvement with native)
4. **Deploy to CI/CD** for multi-platform native binary builds
5. **Monitor production** for any boot failures and fallback usage

