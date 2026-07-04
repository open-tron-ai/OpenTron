# 🚀 QUICK START - 3 Steps to Running Stack

## ⏱️ Time: 15 minutes | Difficulty: Easy

---

## 📋 Prerequisites (One-Time Setup)

Verify you have:
- ✅ Docker installed (`docker --version`)
- ✅ Java 21+ (`java -version`)
- ✅ Maven 3.9+ (bundled in system)
- ✅ Node 24+ (`node -v`)

---

## 🎯 3-Step Startup

### OPTION A: One-Click Start (Recommended)

**Windows:**
```powershell
.\start-stack.bat
```

**macOS/Linux:**
```bash
./start-stack.sh
```

**Done!** Everything starts automatically.

---

### OPTION B: Manual 3-Step Start

#### 1️⃣ Start PostgreSQL (30 seconds)

**Windows:**
```powershell
docker run -d --restart always --name opentron-postgres `
  -e POSTGRES_DB=opentron `
  -e POSTGRES_USER=opentron `
  -e POSTGRES_PASSWORD=opentron_secure_password `
  -p 5432:5432 `
  -v postgres_data:/var/lib/postgresql/data `
  postgres:16-alpine
```

**macOS/Linux:**
```bash
docker run -d --restart always --name opentron-postgres \
  -e POSTGRES_DB=opentron \
  -e POSTGRES_USER=opentron \
  -e POSTGRES_PASSWORD=opentron_secure_password \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16-alpine
```

✅ **PostgreSQL running**

---

#### 2️⃣ Start Backend (1 minute)

**Open Terminal 1 - Windows:**
```powershell
$env:JAVA_HOME = "C:\Users\ciorica\Documents\jdk-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER = "opentron"
$env:POSTGRES_PASSWORD = "opentron_secure_password"
cd java/opentron-java/backend
C:\Users\ciorica\Documents\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run
```

**Open Terminal 1 - macOS/Linux:**
```bash
export POSTGRES_URL="jdbc:postgresql://localhost:5432/opentron"
export POSTGRES_USER="opentron"
export POSTGRES_PASSWORD="opentron_secure_password"
cd java/opentron-java/backend
mvn spring-boot:run
```

✅ **Backend running on http://localhost:8000**

---

#### 3️⃣ Start Frontend (1 minute)

**Open Terminal 2 - Windows:**
```powershell
$env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;" + $env:PATH
$env:PATH = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\bin\Hostx64\x64;" + $env:PATH
$env:LIB = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\lib\x64;C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\um\x64;C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\ucrt\x64;" + $env:LIB
$env:INCLUDE = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\include;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\um;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\shared;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\ucrt"
$env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;C:\Users\ciorica\.cargo\bin;" + $env:PATH
cd frontend
C:\Users\ciorica\Documents\node-v24.18.0-win-x64\npm.cmd run tauri dev
```

**Open Terminal 2 - macOS/Linux:**
```bash
cd frontend
npm run tauri dev
```

✅ **Frontend running (Tauri window opens)**

---

## ✅ Verification

### Check Everything Started:

```bash
# Terminal 3 - Run these commands to verify

# 1. Check PostgreSQL
docker ps | grep postgres
# Should show: opentron-postgres

# 2. Check Backend
curl http://localhost:8000/v1/agents/status
# Should return JSON (not error)

# 3. Check Frontend
# Tauri window should be visible on screen
```

---

## 🎨 Access the Dashboard

1. **Tauri window opens automatically** - Look for the app
2. **Or navigate manually:**
   - Frontend (alternative): http://localhost:5173
3. **Find Storage Dashboard:**
   - Look in menu/navigation
   - Click "Storage" or "Dashboard"
   - You'll see stat cards showing real data from database

---

## 🧪 Test It Works

### Execute an Agent

```bash
curl -X POST http://localhost:8000/v1/agents/coordinate \
  -H "Content-Type: application/json" \
  -d '{"request": "test", "context": ""}'
```

### Watch Dashboard Update
- Open Storage Dashboard
- You'll see traces and stats appear in real-time
- Data is being saved to PostgreSQL

---

## 📊 System Addresses

| Component | Address | Port |
|-----------|---------|------|
| PostgreSQL | localhost | 5432 |
| Backend | http://localhost:8000 | 8000 |
| Frontend | Tauri window | (auto) |
| Frontend alt | http://localhost:5173 | 5173 |

---

## 🔑 Credentials

```
Database: opentron
User:     opentron
Password: opentron_secure_password
```

---

## ⚠️ If Something Fails

### PostgreSQL won't start?
```bash
# Stop existing container
docker rm -f opentron-postgres
# Then run docker run command again
```

### Backend won't start?
```bash
# Make sure PostgreSQL is running first
docker ps | grep postgres
# Then try backend again
```

### Frontend won't start?
```bash
# Make sure backend is running
curl http://localhost:8000/v1/agents/status
# Then try frontend again
```

---

## 📞 Getting Help

1. **Read full guide:** See `HOW_TO_START.md`
2. **Check logs:** Look at terminal output
3. **Verify services:** Run curl commands above
4. **See troubleshooting:** In `HOW_TO_START.md`

---

## 🎊 Done!

### You now have:
- ✅ PostgreSQL database (persistent storage)
- ✅ Backend API (all 25 endpoints working)
- ✅ Frontend UI (with real-time dashboard)
- ✅ Automatic data persistence
- ✅ Real-time storage statistics
- ✅ Complete execution history

### Next:
1. Open Storage Dashboard
2. Execute an agent
3. Watch data appear in real-time
4. Explore the new UI

---

**Questions? See HOW_TO_START.md for detailed setup guide.**

**Ready to go! 🚀**
