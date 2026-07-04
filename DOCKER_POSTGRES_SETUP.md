# PostgreSQL Docker Setup Guide for OpenTron

## ❓ Does PostgreSQL Auto-Start with Docker Desktop?

### Short Answer: **NO** ❌
- PostgreSQL containers do **NOT** auto-start when you start Docker Desktop
- Containers only run when explicitly started
- You need to start it manually or use auto-restart flags

### How to Enable Auto-Start

#### Option 1: Add `--restart always` flag (Recommended)
When creating the container, add the restart policy:
```bash
docker run -d \
  --restart always \
  --name opentron-postgres \
  -e POSTGRES_DB=opentron \
  -e POSTGRES_USER=opentron \
  -e POSTGRES_PASSWORD=opentron_secure_password \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16-alpine
```

**Restart Policies Available:**
- `no` - Do not automatically restart (default)
- `always` - Always restart if stopped
- `unless-stopped` - Always restart unless explicitly stopped
- `on-failure` - Restart only on failure
- `on-failure:5` - Restart max 5 times on failure

#### Option 2: Use Docker Compose (Easier)
Create `docker-compose.yml` and specify restart policy:
```yaml
services:
  postgres:
    image: postgres:16-alpine
    restart: always  # Auto-restart enabled
    # ... rest of config
```

---

## 🚀 Step-by-Step Setup

### **Step 1: Start Docker Desktop**
- Windows: Search "Docker Desktop" and click to start
- macOS: Open Applications > Docker.app
- Wait for it to fully initialize (check tray icon)

### **Step 2: Run the Setup Script**

#### **Windows (PowerShell):**
```powershell
cd C:\Users\ciorica\Documents\OpenTron
.\setup-postgres.bat
```

#### **macOS/Linux:**
```bash
cd ~/Documents/OpenTron  # or wherever it is
chmod +x setup-postgres.sh
./setup-postgres.sh
```

#### **Or Run Manually:**
```bash
docker run -d \
  --restart always \
  --name opentron-postgres \
  -e POSTGRES_DB=opentron \
  -e POSTGRES_USER=opentron \
  -e POSTGRES_PASSWORD=opentron_secure_password \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16-alpine
```

### **Step 3: Verify PostgreSQL is Running**
```bash
# Check container status
docker ps | grep postgres

# View logs
docker logs opentron-postgres

# Connect and test
docker exec -it opentron-postgres psql -U opentron -d opentron -c "SELECT version();"
```

---

## 🔧 Configure Java Backend

### **Option 1: Environment Variables (Recommended)**

#### **Windows (PowerShell):**
```powershell
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER = "opentron"
$env:POSTGRES_PASSWORD = "opentron_secure_password"
$env:ENGINE_HOST = "http://localhost:11434"
```

#### **Windows (Command Prompt):**
```cmd
set POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
set POSTGRES_USER=opentron
set POSTGRES_PASSWORD=opentron_secure_password
set ENGINE_HOST=http://localhost:11434
```

#### **macOS/Linux (Bash):**
```bash
export POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
export POSTGRES_USER=opentron
export POSTGRES_PASSWORD=opentron_secure_password
export ENGINE_HOST=http://localhost:11434
```

#### **Make Permanent (Add to ~/.bashrc or ~/.zshrc):**
```bash
# Add these lines to your shell config file
echo 'export POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron' >> ~/.bashrc
echo 'export POSTGRES_USER=opentron' >> ~/.bashrc
echo 'export POSTGRES_PASSWORD=opentron_secure_password' >> ~/.bashrc
echo 'export ENGINE_HOST=http://localhost:11434' >> ~/.bashrc
source ~/.bashrc
```

### **Option 2: Create .env File**
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

Then edit `.env` with your settings (same values as above).

### **Option 3: Hardcode in application.properties**
The `application.properties` file already has defaults that match Docker:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/opentron
spring.datasource.username=opentron
spring.datasource.password=opentron_secure_password
```

---

## 🔌 Connection Details

Once configured, the Java backend will connect to PostgreSQL with these settings:

```
Protocol:   PostgreSQL
Host:       localhost
Port:       5432
Database:   opentron
User:       opentron
Password:   opentron_secure_password
URL:        jdbc:postgresql://localhost:5432/opentron
```

**application.properties has been pre-configured** to use these values (with fallbacks).

---

## 🏗️ Build and Run Backend

### **With Environment Variables Set:**

#### **Windows (PowerShell):**
```powershell
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java\backend
mvn clean package -DskipTests
mvn spring-boot:run
```

#### **macOS/Linux:**
```bash
cd ~/Documents/OpenTron/java/opentron-java/backend
mvn clean package -DskipTests
mvn spring-boot:run
```

### **Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Application started in 5.234 seconds
[INFO] Configuring PostgreSQL connection pool
[INFO] URL: jdbc:postgresql://localhost:5432/opentron
[INFO] Connection pool initialized: max=20, min=5
[INFO] Flyway: Validating locations of migrations...
[INFO] Flyway: Executing migration V1__Initial_Schema.sql
[INFO] Successfully created tables...
```

---

## ✅ Verify Everything Works

### **Check PostgreSQL Connection:**
```bash
# Connect via Docker
docker exec -it opentron-postgres psql -U opentron -d opentron

# Inside psql prompt:
\dt                    # List tables
SELECT COUNT(*) FROM agent_memory;
SELECT COUNT(*) FROM trace_logs;
\q                     # Quit
```

### **Check Java Backend:**
```bash
# Test health endpoint
curl http://localhost:8000/v1/health

# Expected response:
# {"status":"UP"}
```

### **Check Logs:**
```bash
# PostgreSQL logs
docker logs opentron-postgres

# Backend logs
tail -f logs/opentron-backend.log
```

---

## 🐳 Docker Commands Cheat Sheet

### **Container Management**
```bash
# List all containers (running and stopped)
docker ps -a

# List only running containers
docker ps

# View container logs
docker logs opentron-postgres
docker logs -f opentron-postgres    # Follow in real-time

# Start stopped container
docker start opentron-postgres

# Stop running container
docker stop opentron-postgres

# Restart container
docker restart opentron-postgres

# Remove container (must be stopped first)
docker stop opentron-postgres
docker rm opentron-postgres

# View container statistics
docker stats opentron-postgres
```

### **Database Operations**
```bash
# Connect to database
docker exec -it opentron-postgres psql -U opentron -d opentron

# Run SQL command
docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT version();"

# Check if PostgreSQL is ready
docker exec opentron-postgres pg_isready -U opentron

# View database size
docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT pg_size_pretty(pg_database_size('opentron'));"

# List all databases
docker exec opentron-postgres psql -U opentron -l
```

### **Volume Management**
```bash
# List volumes
docker volume ls | grep postgres

# View volume details
docker volume inspect postgres_data

# Remove volume (deletes all data!)
docker volume rm postgres_data
```

---

## 🆘 Troubleshooting

### **Problem: "Connection refused"**
```bash
# Check if container is running
docker ps | grep postgres

# If not running, start it
docker start opentron-postgres

# Wait a few seconds for PostgreSQL to initialize
sleep 5

# Check logs
docker logs opentron-postgres
```

### **Problem: "Port 5432 already in use"**
```bash
# Find what's using the port
lsof -i :5432  # macOS/Linux
netstat -ano | findstr :5432  # Windows

# Either:
# 1. Stop the process using the port
# 2. Or use a different port: -p 5433:5432
```

### **Problem: "Authentication failed"**
```bash
# Verify credentials match
echo $POSTGRES_USER
echo $POSTGRES_PASSWORD

# Should show: opentron / opentron_secure_password

# If not set, set them:
export POSTGRES_USER=opentron
export POSTGRES_PASSWORD=opentron_secure_password
```

### **Problem: "Container keeps stopping"**
```bash
# Check logs for error
docker logs opentron-postgres

# Common reasons:
# 1. Out of disk space
# 2. Out of memory
# 3. Volume permission issues

# Check logs for specific error
docker inspect opentron-postgres | grep -i error
```

### **Problem: "Flyway migration failed"**
```bash
# Check if tables were created
docker exec opentron-postgres psql -U opentron -d opentron -c "\dt"

# View migration history
docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT * FROM flyway_schema_history;"

# Reset if needed (WARNING: deletes all data)
docker exec opentron-postgres psql -U opentron -d opentron -c "DROP TABLE IF EXISTS flyway_schema_history CASCADE;"
```

---

## 📊 Health Check

Create this test script to verify complete setup:

### **health-check.sh (macOS/Linux):**
```bash
#!/bin/bash
echo "OpenTron Health Check"
echo "===================="
echo ""

# Check Docker
echo "1. Docker Status:"
if docker ps > /dev/null 2>&1; then
    echo "   ✅ Docker is running"
else
    echo "   ❌ Docker is not running"
    exit 1
fi

# Check PostgreSQL container
echo "2. PostgreSQL Container:"
if docker ps | grep -q opentron-postgres; then
    echo "   ✅ Container is running"
else
    echo "   ❌ Container is not running"
    exit 1
fi

# Check PostgreSQL service
echo "3. PostgreSQL Service:"
if docker exec opentron-postgres pg_isready -U opentron > /dev/null 2>&1; then
    echo "   ✅ Service is ready"
else
    echo "   ❌ Service is not ready"
    exit 1
fi

# Check database
echo "4. Database Access:"
if docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT 1;" > /dev/null 2>&1; then
    echo "   ✅ Database is accessible"
else
    echo "   ❌ Database is not accessible"
    exit 1
fi

# Check tables
echo "5. Database Tables:"
TABLES=$(docker exec opentron-postgres psql -U opentron -d opentron -c "\dt" -t | wc -l)
echo "   ✅ Found $TABLES tables"

# Check Java backend
echo "6. Java Backend:"
if lsof -i :8000 > /dev/null 2>&1; then
    echo "   ✅ Backend is running on port 8000"
else
    echo "   ⚠️  Backend is not running (you need to start it)"
fi

echo ""
echo "===================="
echo "✅ All systems operational!"
```

### **health-check.bat (Windows):**
```batch
@echo off
echo OpenTron Health Check
echo ====================
echo.

echo 1. Docker Status:
docker ps > nul 2>&1
if errorlevel 1 (
    echo    X Docker is not running
    exit /b 1
) else (
    echo    [OK] Docker is running
)

echo 2. PostgreSQL Container:
docker ps | findstr "opentron-postgres" > nul
if errorlevel 1 (
    echo    X Container is not running
    exit /b 1
) else (
    echo    [OK] Container is running
)

echo 3. PostgreSQL Service:
docker exec opentron-postgres pg_isready -U opentron > nul 2>&1
if errorlevel 1 (
    echo    X Service is not ready
    exit /b 1
) else (
    echo    [OK] Service is ready
)

echo 4. Database Access:
docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT 1;" > nul 2>&1
if errorlevel 1 (
    echo    X Database is not accessible
    exit /b 1
) else (
    echo    [OK] Database is accessible
)

echo.
echo ====================
echo [SUCCESS] All systems operational!
pause
```

---

## 📝 Summary

| Task | Windows | macOS/Linux |
|------|---------|------------|
| Run setup | `setup-postgres.bat` | `./setup-postgres.sh` |
| Set env vars | PowerShell: `$env:VAR=value` | Bash: `export VAR=value` |
| Start Docker | Search "Docker Desktop" | `open /Applications/Docker.app` |
| Build backend | `mvn clean package` | `mvn clean package` |
| Run backend | `mvn spring-boot:run` | `mvn spring-boot:run` |
| Check logs | `docker logs opentron-postgres` | `docker logs opentron-postgres` |
| Connect to DB | `docker exec -it opentron-postgres psql -U opentron -d opentron` | Same |

---

## ✨ You're Ready!

1. ✅ Docker installed
2. ✅ PostgreSQL configured
3. ✅ Environment setup
4. ✅ Java backend pre-configured
5. ✅ Flyway migrations ready

**Next Step:** Run `setup-postgres.bat` (Windows) or `./setup-postgres.sh` (macOS/Linux)
