# 🚀 How to Run OpenTron 1.0.0

## Quick Start (2 minutes)

### 1. Extract the ZIP
```
C:\Users\ciorica\Downloads\OpenTron-1.0.0.zip
→ Right-click → Extract All
→ Creates: C:\Users\ciorica\Downloads\OpenTron-1.0.0\
```

### 2. Open PowerShell in the folder
```powershell
cd C:\Users\ciorica\Downloads\OpenTron-1.0.0
```

### 3. Start the Backend
```powershell
java -jar java/opentron-java/backend/target/*.jar
```

### 4. Open in Browser
```
http://localhost:7200
```

That's it! OpenTron is running.

---

## Running Options

### Option 1: Backend Only (Simplest)
```powershell
cd C:\Users\ciorica\Downloads\OpenTron-1.0.0
java -jar java/opentron-java/backend/target/*.jar
```
**Access:** http://localhost:7200
**What runs:** Java API server only (web UI served from server)

---

### Option 2: Desktop App (Full Native UI)
Requires: Rust/Cargo installed

```powershell
cd C:\Users\ciorica\Downloads\OpenTron-1.0.0\desktop\src-tauri
cargo build --release
cargo run --release
```

**What runs:** Native Tauri window with embedded backend
**Startup time:** 0.5-1.5s (with GraalVM native binary)

---

### Option 3: Web UI Only (Development Mode)
Requires: Node.js + npm

```powershell
cd C:\Users\ciorica\Downloads\OpenTron-1.0.0\frontend
npm install
npm run dev
```

**Access:** http://localhost:5173 (or shown in terminal)
**Note:** Requires backend running separately (Option 1)

---

### Option 4: Docker Compose (Recommended for Production)
Requires: Docker installed

```powershell
cd C:\Users\ciorica\Downloads\OpenTron-1.0.0\deploy\docker
docker-compose up
```

**Access:** http://localhost:7200
**What runs:** Backend in Docker container

---

## Prerequisites

### For Option 1 (Backend Only) - MINIMUM
- ✓ Java 21+ (verify: `java -version`)

### For Option 2 (Desktop App)
- ✓ Java 21+
- ✓ Rust/Cargo (verify: `cargo --version`)
- ✓ Node.js v18+ (verify: `npm --version`)

### For Option 3 (Web UI)
- ✓ Node.js v18+
- ✓ Backend running separately (Option 1)

### For Option 4 (Docker)
- ✓ Docker installed

---

## Troubleshooting

### "java: command not found"
Java is not installed or not in PATH.
```powershell
# Verify Java is installed
java -version

# If not found, set JAVA_HOME
$env:JAVA_HOME = "C:\path\to\java21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### "Backend failed to start" or port 7200 already in use
```powershell
# Find what's using port 7200
netstat -ano | findstr :7200

# Kill the process (if needed)
taskkill /PID <PID> /F

# Try different port
java -jar java/opentron-java/backend/target/*.jar --server.port=7201
```

### "npm: command not found"
Node.js is not installed.
Download from: https://nodejs.org/

### "cargo: command not found"
Rust is not installed.
Download from: https://rustup.rs/

---

## Common Issues

| Issue | Solution |
|-------|----------|
| "Cannot find JAR file" | Ensure you're in the OpenTron-1.0.0 folder |
| "Port 7200 already in use" | Change port or kill process using it |
| "Out of memory" | Increase Java heap: `java -Xmx4G -jar ...` |
| "Connection refused" | Backend not running, check logs |
| "Frontend not loading" | Verify backend is responsive: `curl http://localhost:7200` |

---

## Stopping OpenTron

Press **Ctrl+C** in the terminal where it's running.

---

## Configuration

### Environment Variables
Edit `.env` or set before running:
```powershell
$env:API_PORT = "7200"
$env:DATABASE_URL = "sqlite://./opentron.db"
$env:LOG_LEVEL = "info"

java -jar java/opentron-java/backend/target/*.jar
```

### Config Files
- `configs/opentron/config.toml` — Main configuration
- `.env.example` — Environment template

---

## Testing Connection

After starting, verify it's running:

```powershell
# Test API endpoint
curl http://localhost:7200/api/health

# Should return JSON response with status
```

---

## Next Steps

1. ✅ Run backend (Option 1)
2. 🌐 Open http://localhost:7200
3. ⚙️ Configure settings
4. 📊 Set up data sources
5. 🤖 Start using agents

---

## Documentation

For more details, see:
- `README.md` — Project overview
- `DEPLOYMENT_GUIDE.md` — Production deployment
- `deploy/docker/README.md` — Docker setup
- `configs/opentron/examples/` — Configuration examples

