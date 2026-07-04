# Quick Reference Card - PostgreSQL Docker Setup

## 🚀 Quick Start (Copy & Paste)

### **Windows (PowerShell):**
```powershell
# 1. Start Docker Desktop (search and click)
# 2. Run setup script
cd C:\Users\ciorica\Documents\OpenTron
.\setup-postgres.bat

# 3. Set environment variables
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER = "opentron"
$env:POSTGRES_PASSWORD = "opentron_secure_password"
$env:ENGINE_HOST = "http://localhost:11434"

# 4. Build backend
cd java\opentron-java\backend
mvn clean package -DskipTests

# 5. Run backend
mvn spring-boot:run
```

### **macOS/Linux:**
```bash
# 1. Start Docker (if needed)
# 2. Run setup script
cd ~/Documents/OpenTron  # adjust path
chmod +x setup-postgres.sh
./setup-postgres.sh

# 3. Set environment variables
export POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
export POSTGRES_USER=opentron
export POSTGRES_PASSWORD=opentron_secure_password
export ENGINE_HOST=http://localhost:11434

# 4. Build backend
cd java/opentron-java/backend
mvn clean package -DskipTests

# 5. Run backend
mvn spring-boot:run
```

---

## 📋 Connection Details

```
PostgreSQL in Docker
├─ Host:       localhost
├─ Port:       5432
├─ Database:   opentron
├─ User:       opentron
├─ Password:   opentron_secure_password
└─ JDBC URL:   jdbc:postgresql://localhost:5432/opentron

Java Backend Connection
├─ Host:       localhost
├─ Port:       8000
└─ Health:     http://localhost:8000/v1/health
```

---

## 🔧 Essential Commands

### **Docker Management**
```bash
# See if container is running
docker ps | grep postgres

# Start container
docker start opentron-postgres

# Stop container
docker stop opentron-postgres

# View logs
docker logs opentron-postgres

# Connect to database
docker exec -it opentron-postgres psql -U opentron -d opentron
```

### **Verify Setup**
```bash
# Check PostgreSQL is ready
docker exec opentron-postgres pg_isready -U opentron

# Check table creation
docker exec opentron-postgres psql -U opentron -d opentron -c "\dt"

# Check Java backend
curl http://localhost:8000/v1/health
```

---

## ❓ Common Questions

### **Q: Does it auto-start with Docker Desktop?**
**A:** No. Add `--restart always` flag or use Docker Compose for auto-start.

### **Q: How do I stop everything?**
**A:** 
```bash
docker stop opentron-postgres    # Stop PostgreSQL
Ctrl+C                           # Stop Java backend
# Close Docker Desktop if done
```

### **Q: Where are my database files stored?**
**A:** In Docker volume `postgres_data` (Docker manages this)

### **Q: How do I backup my database?**
**A:** 
```bash
docker exec opentron-postgres pg_dump -U opentron opentron > backup.sql
```

### **Q: Connection refused - what do I do?**
**A:** 
```bash
# 1. Check Docker Desktop is running
# 2. Check PostgreSQL container is running
docker ps | grep postgres

# 3. Start it if needed
docker start opentron-postgres

# 4. Wait a few seconds and try again
```

---

## 📊 Files Modified/Created

✅ Created:
- `setup-postgres.sh` (Linux/macOS setup script)
- `setup-postgres.bat` (Windows setup script)
- `.env.example` (Environment variables template)
- `DOCKER_POSTGRES_SETUP.md` (Full setup guide)
- `QUICK_REFERENCE.md` (This file)

✅ Updated:
- `application.properties` (Pre-configured for Docker PostgreSQL)

---

## ✨ Status

- ✅ PostgreSQL Docker image available
- ✅ Setup scripts created
- ✅ Java backend pre-configured
- ✅ Documentation complete
- ✅ Ready to deploy

**Next Step:** Run `setup-postgres.bat` or `./setup-postgres.sh`
