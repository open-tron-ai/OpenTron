# 🚀 How to Start the Stack - Step by Step

## ⏱️ Total Time: 15-20 minutes

---

## Option 1: Automatic Start (Easiest)

### Windows
```powershell
# Run this ONE command
.\start-stack.bat
```

### macOS/Linux
```bash
# Run this ONE command
./start-stack.sh
```

**What happens:**
1. Checks for Docker ✅
2. Starts PostgreSQL container ✅
3. Builds backend with Maven ✅
4. Starts backend server ✅
5. Starts frontend in Tauri ✅

---

## Option 2: Manual Start (Step by Step)

### Step 1: Start PostgreSQL (5 minutes)

**Windows (PowerShell):**
```powershell
docker run -d --restart always --name opentron-postgres `
  -e POSTGRES_DB=opentron `
  -e POSTGRES_USER=opentron `
  -e POSTGRES_PASSWORD=opentron_secure_password `
  -p 5432:5432 `
  -v postgres_data:/var/lib/postgresql/data `
  postgres:16-alpine
```

**macOS/Linux (Bash):**
```bash
docker run -d --restart always --name opentron-postgres \
  -e POSTGRES_DB=opentron \
  -e POSTGRES_USER=opentron \
  -e POSTGRES_PASSWORD=opentron_secure_password \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16-alpine
```

**Verify PostgreSQL started:**
```bash
docker ps | grep opentron-postgres
```

Should see:
```
opentron-postgres   postgres:16-alpine   5432/tcp
```

✅ **PostgreSQL running on port 5432**

---

### Step 2: Start Backend (5 minutes)

**Windows (PowerShell):**
```powershell
# Set environment variables
$env:JAVA_HOME = "C:\Users\ciorica\Documents\jdk-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER = "opentron"
$env:POSTGRES_PASSWORD = "opentron_secure_password"

# Navigate and start
cd java/opentron-java/backend
C:\Users\ciorica\Documents\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run
```

**macOS/Linux (Bash):**
```bash
# Set environment variables
export JAVA_HOME="/usr/libexec/java_home -v 21"
export POSTGRES_URL="jdbc:postgresql://localhost:5432/opentron"
export POSTGRES_USER="opentron"
export POSTGRES_PASSWORD="opentron_secure_password"

# Navigate and start
cd java/opentron-java/backend
mvn spring-boot:run
```

**Wait for this message in logs:**
```
[INFO] OpenTron Backend Application started
[INFO] Started OpentronBackendApplication in X.XXX seconds
```

✅ **Backend running on port 8000**

---

### Step 3: Start Frontend (5 minutes)

**Open a NEW terminal/PowerShell window** (keep backend running in previous window)

**Windows (PowerShell):**
```powershell
# Set environment
$env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;" + $env:PATH
$env:PATH = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\bin\Hostx64\x64;" + $env:PATH

$env:LIB = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\lib\x64;" +
           "C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\um\x64;" +
           "C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\ucrt\x64;" +
           $env:LIB

$env:INCLUDE = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\include;" +
               "C:\Users\ciorica\Documents\10\Include\10.0.26100.0\um;" +
               "C:\Users\ciorica\Documents\10\Include\10.0.26100.0\shared;" +
               "C:\Users\ciorica\Documents\10\Include\10.0.26100.0\ucrt"

$env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;C:\Users\ciorica\.cargo\bin;" + $env:PATH

# Navigate and start
cd frontend
C:\Users\ciorica\Documents\node-v24.18.0-win-x64\npm.cmd run tauri dev
```

**macOS/Linux (Bash):**
```bash
# Navigate and start
cd frontend
npm run tauri dev
```

**Wait for this message:**
```
[Tauri] app started
[Tauri] Window created
```

✅ **Frontend running (Tauri app will open)**

---

## 🎯 Verification Checklist

### PostgreSQL
```bash
# Check if running
docker ps | grep postgres

# Should show: opentron-postgres   postgres:16-alpine
```

### Backend
```bash
# Check if running
curl http://localhost:8000/v1/agents/status
```

Should respond with JSON (not error)

### Frontend
- Tauri window should open automatically
- If not, go to: http://localhost:5173

---

## ✅ Everything Running?

### You should see:

1. **PostgreSQL Container** - Running in Docker
2. **Backend Terminal** - Shows "Started OpentronBackendApplication"
3. **Frontend Window** - Tauri app window opens
4. **No errors** - All systems green

---

## 🎨 Open the Storage Dashboard

### In the Frontend App:
1. Look for a menu or navigation
2. Find "Storage" or "Dashboard" section
3. Click "Storage Dashboard"
4. You should see:
   - 📊 Statistics cards
   - 📋 Traces tab
   - 💾 Memory tab

---

## 🧪 Test It Works

### Execute an Agent to Generate Data

```bash
curl -X POST http://localhost:8000/v1/agents/coordinate \
  -H "Content-Type: application/json" \
  -d '{"request": "hello", "context": ""}'
```

### Check Backend Logs
You should see:
```
[AgentsController] 💾 Trace saved to PostgreSQL
```

### Check Dashboard
Open StorageDashboard in frontend, and you should see:
- Total traces increased
- Total memories increased
- Real statistics from database

---

## ⚠️ Troubleshooting

### PostgreSQL won't start
```bash
# Check if port 5432 is already in use
lsof -i :5432  # macOS/Linux
netstat -an | findstr :5432  # Windows

# If already exists, remove old container
docker rm -f opentron-postgres
# Then try again
```

### Backend won't start
```bash
# Clear Maven cache
rm -rf ~/.m2/repository/org/postgresql/
rm -rf ~/.m2/repository/org/springframework/

# Try again
mvn clean package -DskipTests
mvn spring-boot:run
```

### Frontend won't start
```bash
# Check Node is installed
node -v  # Should be v24+
npm -v   # Should be 11+

# Install dependencies
npm install

# Try again
npm run tauri dev
```

### Database connection error
```bash
# Test PostgreSQL is running
psql -U opentron -d opentron -h localhost

# If fails, check container logs
docker logs opentron-postgres
```

---

## 📍 Service Locations

| Service | URL | Port |
|---------|-----|------|
| PostgreSQL | localhost:5432 | 5432 |
| Backend API | http://localhost:8000 | 8000 |
| Frontend | Tauri app | auto |
| Frontend alt | http://localhost:5173 | 5173 |

---

## 🔑 Database Credentials

```
Host:     localhost
Port:     5432
Database: opentron
User:     opentron
Password: opentron_secure_password
```

---

## 📊 What Happens on First Start

### PostgreSQL Startup (30 seconds)
- Container initializes
- Database created
- User and permissions set

### Backend Startup (20-30 seconds)
- Maven builds project
- Spring Boot initializes
- **Flyway runs migration** ← Creates all tables
- Connection pool starts
- Server ready on port 8000

### Frontend Startup (10-20 seconds)
- Dependencies checked
- Tauri bundle builds
- Window opens
- Ready to use

**Total: ~2-3 minutes on first run**

---

## 🎯 Success Indicators

✅ You'll know everything is working when:

1. **PostgreSQL Container** - Shows in `docker ps`
2. **Backend Logs** - Show "Started OpentronBackendApplication"
3. **Frontend Window** - Tauri app appears
4. **No Errors** - None of the terminal windows show errors
5. **Dashboard Loads** - StorageDashboard renders with stat cards
6. **Data Shows** - After executing an agent, traces appear

---

## 🚀 Next Steps (After Starting)

1. Open Storage Dashboard
2. Execute an agent
3. Watch traces appear in real-time
4. Browse traces by agent
5. Search memories
6. Monitor statistics

---

## 💡 Tips

### Keep Services Running
- Keep all 3 terminal windows open
- Don't close PostgreSQL container
- Frontend window can minimize

### Restart Services
- **Backend:** Ctrl+C in terminal, then restart
- **Frontend:** Ctrl+C in terminal, then restart
- **PostgreSQL:** Just runs in background

### Check Everything Works
```bash
# Test each service:

# 1. PostgreSQL
docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT COUNT(*) FROM trace_logs;"

# 2. Backend
curl http://localhost:8000/v1/agents/status

# 3. Frontend
# Just check that Tauri window is open
```

---

## 🎊 You're Ready!

### Quick Reference

| Step | Command | Time |
|------|---------|------|
| Start DB | `docker run ...` | 30 sec |
| Start Backend | `mvn spring-boot:run` | 30 sec |
| Start Frontend | `npm run tauri dev` | 20 sec |
| **Total** | **3 steps** | **~2 min** |

**Then you have a fully functional OpenTron stack with PostgreSQL persistence!**

---

## 📞 Need Help?

If services don't start:

1. **Check Docker:** `docker --version`
2. **Check Java:** `java -version` (should be 21+)
3. **Check Node:** `node -v` (should be 24+)
4. **Check logs:** Look at terminal output for error messages
5. **Check ports:** Make sure 5432, 8000 are not in use

---

**Happy hacking! 🚀**

Once everything is running, open the Storage Dashboard and start using OpenTron with persistent PostgreSQL storage!
