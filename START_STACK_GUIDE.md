# 🚀 Using start-stack.bat - Complete Guide

## ⏱️ Total Time: 2-3 minutes

---

## What start-stack.bat Does

The batch script automates everything:

1. ✅ Checks Docker is installed
2. ✅ Starts PostgreSQL container
3. ✅ Builds backend with Maven
4. ✅ Starts backend in separate window
5. ✅ Starts frontend in separate window

---

## 🎯 How to Use

### Step 1: Open PowerShell or Command Prompt

Navigate to the OpenTron project root directory:

```powershell
cd C:\Users\ciorica\Documents\OpenTron
```

Verify you're in the right place (should see `start-stack.bat`):

```powershell
ls start-stack.bat
```

---

### Step 2: Run the Script

```powershell
.\start-stack.bat
```

**That's it!** The script handles everything.

---

## 📊 What You'll See

### Main Window Output:

```
════════════════════════════════════════════════════
  OpenTron - PostgreSQL Integration Stack
  Backend + Frontend + Database
════════════════════════════════════════════════════

[1/5] Checking Docker...
OK: Docker found

[2/5] Starting PostgreSQL...
Starting PostgreSQL container...
Waiting for PostgreSQL to start...
OK: PostgreSQL started on port 5432

[3/5] Building Backend...
Building with Maven...
OK: Backend built successfully

[4/5] Starting Backend (in new window)...
OK: Backend starting in separate window
Waiting 15 seconds for backend to start...

[5/5] Starting Frontend (in new window)...
OK: Frontend starting in separate window

════════════════════════════════════════════════════
   ✅ OpenTron Stack Started Successfully!
════════════════════════════════════════════════════

   Services:
   - PostgreSQL:  http://localhost:5432
   - Backend:     http://localhost:8000
   - Frontend:    Tauri window (auto-opens)

   [Press any key to close this window]
```

---

### Three New Windows Will Open:

1. **OpenTron Backend** window
   - Shows Spring Boot startup logs
   - Shows "Started OpentronBackendApplication"

2. **OpenTron Frontend** window
   - Shows Tauri dev server logs
   - App window should open automatically

3. **Main window** (where you ran the script)
   - Shows success message
   - Can close after reading

---

## ✅ Verification

### All Services Running?

#### PostgreSQL:
```powershell
docker ps | findstr opentron-postgres
```

Should show:
```
CONTAINER ID   IMAGE                 PORTS                    
abc123         postgres:16-alpine    0.0.0.0:5432->5432/tcp
```

#### Backend:
- Check "OpenTron Backend" window
- Should show: `Started OpentronBackendApplication in X.XXX seconds`

#### Frontend:
- Tauri window should be visible on screen
- Or check "OpenTron Frontend" window for logs

---

## 🧪 Test It Works

### In PowerShell:

```powershell
# Test backend is responding
curl http://localhost:8000/v1/agents/status

# Should return JSON (not an error)
```

### In the Frontend:

1. Look for Storage Dashboard in the menu
2. You should see statistics cards
3. Execute an agent:

```powershell
curl -X POST http://localhost:8000/v1/agents/coordinate `
  -H "Content-Type: application/json" `
  -d '{"request": "test", "context": ""}'
```

4. Watch the dashboard - traces should appear in real-time

---

## ⚠️ Common Issues

### Issue 1: "Docker is not installed"

**Error Message:**
```
ERROR: Docker is not installed or not in PATH
```

**Solution:**
1. Install Docker Desktop: https://www.docker.com/products/docker-desktop
2. Restart your computer
3. Run `start-stack.bat` again

---

### Issue 2: "Backend build failed"

**Error Message:**
```
ERROR: Backend build failed
```

**Solution:**
1. Check your internet (Maven downloads dependencies)
2. Clear Maven cache:
   ```powershell
   rm -r $env:USERPROFILE\.m2\repository\org\postgresql\
   ```
3. Run script again

---

### Issue 3: Frontend window doesn't appear

**Solution:**
1. Wait 30-60 seconds (Tauri takes time to build first time)
2. Check "OpenTron Frontend" window for logs
3. If stuck, manually start:
   ```powershell
   cd frontend
   $env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;" + $env:PATH
   npm run tauri dev
   ```

---

### Issue 4: Backend window closes immediately

**Error in Backend window:**
```
ERROR: Could not connect to PostgreSQL
```

**Solution:**
1. Wait 5 more seconds (PostgreSQL takes time to start)
2. Verify PostgreSQL is running: `docker ps`
3. Manually run backend:
   ```powershell
   cd java/opentron-java/backend
   $env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
   $env:POSTGRES_USER = "opentron"
   $env:POSTGRES_PASSWORD = "opentron_secure_password"
   mvn spring-boot:run
   ```

---

### Issue 5: Port already in use

**Error:**
```
Address already in use: bind
```

**Solution:**
1. Find what's using port 5432, 8000, or 3000
2. Either stop it or kill existing containers:
   ```powershell
   docker rm -f opentron-postgres
   ```
3. Run script again

---

## 🎯 What Each Window Shows

### Backend Window ("OpenTron Backend")

**You'll see:**
```
[INFO] Scanning for projects...
[INFO] Building opentron-java-backend 0.1.0
...
[INFO] Configuring PostgreSQL connection pool
[INFO] URL: jdbc:postgresql://localhost:5432/opentron
[INFO] Connection pool initialized: max=20, min=5
[INFO] Flyway: Executing migration V1__Initial_Schema.sql
[INFO] Successfully created tables: agent_memory, trace_logs, ...
[INFO] Started OpentronBackendApplication in 15.234 seconds
```

**Keep this window open** - it shows backend logs

---

### Frontend Window ("OpenTron Frontend")

**You'll see:**
```
  ➜  Local:   http://localhost:5173/
  ➜  press h + enter to show help
```

**Plus a Tauri app window will open**

**Keep this window open** - it shows frontend logs

---

## 🔄 Managing the Stack

### Keep Things Running

✅ **Leave all 3 windows open**
- Don't close the backend window
- Don't close the frontend window
- Don't close the main window

### Stop Everything

**Close in this order:**
1. Close Frontend window (Tauri app)
2. Close Backend window (Spring Boot)
3. Close main window
4. PostgreSQL keeps running (that's fine, it restarts automatically)

### Restart Services

**Backend only:**
- Close "OpenTron Backend" window
- Main window will show script is still waiting
- Run `start-stack.bat` again (or just restart backend manually)

**Frontend only:**
- Close Frontend window
- Run `npm run tauri dev` in new PowerShell

### Stop PostgreSQL

```powershell
docker stop opentron-postgres
```

Start it again:
```powershell
docker start opentron-postgres
```

---

## 📊 Service Status

### Check Everything at a Glance

**PowerShell:**
```powershell
# 1. PostgreSQL
docker ps -a | findstr opentron

# 2. Backend - check if responsive
(Invoke-WebRequest http://localhost:8000/v1/agents/status -ErrorAction SilentlyContinue).StatusCode

# 3. Frontend - check if Tauri is running
Get-Process node -ErrorAction SilentlyContinue
```

---

## 🎨 After Startup

### Open Storage Dashboard

1. **In Tauri window:** Look for menu/navigation
2. **Find:** "Storage" or "Dashboard" section
3. **Click:** "Storage Dashboard"
4. **See:** Real-time statistics and traces

### Execute an Agent

```powershell
curl -X POST http://localhost:8000/v1/agents/coordinate `
  -H "Content-Type: application/json" `
  -d '{"request": "hello world", "context": ""}'
```

### Watch Data Appear

- Backend window shows: `[AgentsController] 💾 Trace saved to PostgreSQL`
- Dashboard updates with new trace
- Statistics show increased count

---

## 📈 Performance Times

| Step | Time | Notes |
|------|------|-------|
| Docker check | 5 sec | Instant if Docker running |
| PostgreSQL start | 5 sec | Quick startup |
| Maven build | 30-60 sec | First time slower |
| Backend start | 15-20 sec | Spring Boot initialization |
| Frontend start | 20-30 sec | Tauri build first time |
| **Total** | **2-3 min** | **Subsequent runs: 1-2 min** |

---

## 💾 Data Persistence

### Your Data is Saved in:

1. **PostgreSQL Container:** `postgres_data` volume
2. **Survives:** Restarts of any window
3. **Survives:** Docker stop/start
4. **Lost:** Only if you delete the container

### Reset Data

```powershell
# Stop the database
docker stop opentron-postgres

# Remove the container
docker rm -f opentron-postgres

# Delete the volume (WARNING: deletes data!)
docker volume rm postgres_data

# Next run of script creates fresh database
```

---

## 🔑 Database Access

### From PowerShell:

```powershell
# Connect to database
psql -U opentron -d opentron -h localhost

# Inside psql, useful commands:
# \dt                    - List tables
# SELECT COUNT(*) FROM trace_logs;  - Count traces
# \q                     - Quit
```

### From Backend Code:

All 25 endpoints now have database access:
- `GET /v1/agents/storage/stats` - Statistics
- `GET /v1/traces` - Real traces (not mock)
- `POST /v1/memory/store` - Save memories
- etc.

---

## 🎊 Success Checklist

After running `start-stack.bat`:

- [x] Script runs without errors
- [x] PostgreSQL window says "OK: PostgreSQL started"
- [x] Backend window opens and shows Spring Boot starting
- [x] Backend shows "Started OpentronBackendApplication"
- [x] Frontend window opens with Tauri app
- [x] Tauri app window is visible on screen
- [x] Main window shows success message
- [x] `curl http://localhost:8000/v1/agents/status` works

✅ **If all checks pass: You're good to go!**

---

## 📞 If Something Doesn't Work

### Debugging Steps:

1. **Check logs in each window** - Usually shows the error
2. **Test individually:**
   ```powershell
   docker ps           # PostgreSQL running?
   curl http://localhost:8000/v1/agents/status  # Backend working?
   ```
3. **See troubleshooting section above** for specific errors
4. **Read HOW_TO_START.md** for more detailed setup

---

## 🎯 Quick Reference

| Command | What it Does |
|---------|-------------|
| `.\start-stack.bat` | Start everything (recommended) |
| `docker ps` | Check running containers |
| `docker logs opentron-postgres` | See PostgreSQL logs |
| `curl http://localhost:8000/v1/agents/status` | Test backend |
| `psql -U opentron -d opentron` | Connect to database |

---

## 🚀 You're Ready!

Just run:

```powershell
.\start-stack.bat
```

And everything starts automatically. The script handles:
- ✅ Docker
- ✅ PostgreSQL
- ✅ Backend build
- ✅ Backend startup
- ✅ Frontend startup

All in one command!

---

**Happy hacking! 🎉**
