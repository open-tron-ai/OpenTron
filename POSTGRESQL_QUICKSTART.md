# PostgreSQL Storage Integration - Quick Start Guide

## What Was Added

This integration adds persistent PostgreSQL storage to OpenTron for:
- **Agent Memory** - Compressed summaries + raw execution traces
- **Trace Logs** - Detailed execution logs with timestamps
- **Skills Registry** - Skill manifests and versions
- **Document Indexes** - Vector storage for retrieval
- **Automatic Compression** - Zstd compression for large traces
- **Deduplication** - SHA256 hashing prevents duplicate storage
- **Data Retention** - Automatic archival of old data

## Files Created

### Configuration
- `backend/src/main/java/org/opentron/backend/config/StorageConfig.java`
  - HikariCP connection pool configuration
  - PostgreSQL DataSource bean

### JPA Entities
- `backend/src/main/java/org/opentron/backend/storage/entities/AgentMemory.java`
  - Agent memory storage entity with embeddings
- `backend/src/main/java/org/opentron/backend/storage/entities/TraceLog.java`
  - Trace log storage entity with compression support

### Repositories
- `backend/src/main/java/org/opentron/backend/storage/repositories/AgentMemoryRepository.java`
  - CRUD + custom queries for agent memory
- `backend/src/main/java/org/opentron/backend/storage/repositories/TraceLogRepository.java`
  - CRUD + custom queries for trace logs

### Services
- `backend/src/main/java/org/opentron/backend/storage/service/CompressionService.java`
  - Zstd compression/decompression
- `backend/src/main/java/org/opentron/backend/storage/service/StorageService.java`
  - Main storage layer with deduplication & archival

### Database
- `backend/src/main/resources/db/migration/V1__Initial_Schema.sql`
  - Flyway migration script creating all tables & indexes

### Configuration Files
- `backend/pom.xml` (UPDATED)
  - Added PostgreSQL, Spring Data JPA, Flyway, Zstd dependencies
- `backend/src/main/resources/application.properties` (UPDATED)
  - PostgreSQL connection settings
  - JPA/Hibernate configuration
  - Flyway migration settings

---

## Step 1: Install PostgreSQL Locally

### Option A: Docker (Recommended)
```bash
docker run --name opentron-postgres \
  -e POSTGRES_DB=opentron \
  -e POSTGRES_USER=opentron \
  -e POSTGRES_PASSWORD=opentron_secure_password \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  -d postgres:16-alpine
```

### Option B: Install Natively
```bash
# macOS
brew install postgresql@15
brew services start postgresql@15

# Windows (using installer)
# Download from https://www.postgresql.org/download/windows/

# Linux (Ubuntu/Debian)
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Create Database
```bash
psql -U postgres -h localhost
CREATE DATABASE opentron;
CREATE USER opentron WITH PASSWORD 'opentron_secure_password';
GRANT ALL PRIVILEGES ON DATABASE opentron TO opentron;
\q
```

---

## Step 2: Set Environment Variables

### macOS/Linux
```bash
export POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
export POSTGRES_USER=opentron
export POSTGRES_PASSWORD=opentron_secure_password
export ENGINE_HOST=http://localhost:11434
```

### Windows (PowerShell)
```powershell
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER = "opentron"
$env:POSTGRES_PASSWORD = "opentron_secure_password"
$env:ENGINE_HOST = "http://localhost:11434"
```

### Or Create `.env` File
```bash
POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
POSTGRES_USER=opentron
POSTGRES_PASSWORD=opentron_secure_password
ENGINE_HOST=http://localhost:11434
```

---

## Step 3: Rebuild the Backend

```bash
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java\backend

# Clean and compile
mvn clean package -DskipTests

# Or just compile (faster for development)
mvn clean compile -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXXs
```

If you get dependency errors, run:
```bash
mvn dependency:resolve
```

---

## Step 4: Start the Application

### Run from IDE
- Right-click `OpentronBackendApplication.java`
- Select "Run"

### Run from Command Line
```bash
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java\backend
mvn spring-boot:run -Dspring-boot.run.arguments="--engine.host=http://localhost:11434"
```

### Expected Startup Logs
```
[INFO] Starting OpentronBackendApplication...
[INFO] Configuring PostgreSQL connection pool
[INFO] URL: jdbc:postgresql://localhost:5432/opentron
[INFO] Connection pool initialized: max=20, min=5
[INFO] Flyway: Validating locations of migrations...
[INFO] Flyway: Executing migration V1__Initial_Schema.sql
[INFO] Successfully created tables: agent_memory, trace_logs, skills, document_index
```

---

## Step 5: Verify Database Setup

### Connect to PostgreSQL
```bash
psql -U opentron -d opentron -h localhost
```

### Check Tables
```sql
\dt                    -- List all tables
SELECT * FROM agent_memory LIMIT 5;
SELECT * FROM trace_logs LIMIT 5;
```

### Check Migration Status
```sql
SELECT * FROM flyway_schema_history;
```

---

## Step 6: Integration with Existing Code

### Example: Save a Trace
File: `backend/src/main/java/org/opentron/backend/controllers/AgentsController.java`

```java
@Autowired
private StorageService storageService;

@PostMapping("/{id}/execute")
public Mono<ResponseEntity<?>> executeAgent(@PathVariable String id, @RequestBody Map<String, Object> request) {
    // ... existing logic ...
    
    return result.doOnNext(response -> {
        try {
            String input = (String) request.get("input");
            String output = "...response content...";
            storageService.saveTrace(id, input, output, durationMs);
            System.out.println("[AgentsController] Saved trace for " + id);
        } catch (Exception e) {
            System.err.println("[AgentsController] Failed to save: " + e.getMessage());
        }
    });
}
```

### Example: Load Memory
```java
// In any service/controller
@Autowired
private StorageService storageService;

public void loadAgentMemory(String agentName) {
    List<AgentMemory> memory = storageService.loadAgentMemory(agentName, 10);
    for (AgentMemory m : memory) {
        System.out.println(m.getCompressedSummary());
    }
}
```

---

## Storage API Reference

### Save Operations
```java
// Save agent memory
AgentMemory mem = storageService.saveAgentMemory(
    "my-agent",
    "raw execution trace...",
    "compressed summary"
);

// Save trace with auto-compression
TraceLog trace = storageService.saveTrace(
    "my-agent",
    "input data...",
    "output data...",
    1250  // duration in ms
);
```

### Load Operations
```java
// Load recent memory (limit 50 entries)
List<AgentMemory> memory = storageService.loadAgentMemory("my-agent", 50);

// Load recent traces (limit 100 entries)
List<TraceLog> traces = storageService.loadTraces("my-agent", 100);

// Get all traces in time range
List<TraceLog> ranged = storageService.getTracesInRange(
    "my-agent",
    LocalDateTime.now().minusDays(7),
    LocalDateTime.now()
);

// Count entries
long memoryCount = storageService.countAgentMemory("my-agent");
long traceCount = storageService.countAgentTraces("my-agent");
```

### Maintenance Operations
```java
// Archive memory older than 30 days
storageService.archiveOldMemory(30);

// Archive traces older than 7 days
storageService.archiveOldTraces(7);

// Get storage statistics
StorageService.StorageStats stats = storageService.getStorageStats();
System.out.println("Total memories: " + stats.totalMemoryEntries);
System.out.println("Total traces: " + stats.totalTraceEntries);
```

---

## Docker Compose Setup

Create `docker-compose.yml` in project root:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: opentron-postgres
    environment:
      POSTGRES_DB: opentron
      POSTGRES_USER: opentron
      POSTGRES_PASSWORD: opentron_secure_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U opentron"]
      interval: 10s
      timeout: 5s
      retries: 5

  opentron-backend:
    build:
      context: ./java/opentron-java/backend
      dockerfile: Dockerfile
    ports:
      - "8000:8000"
    environment:
      POSTGRES_URL: jdbc:postgresql://postgres:5432/opentron
      POSTGRES_USER: opentron
      POSTGRES_PASSWORD: opentron_secure_password
      ENGINE_HOST: http://ollama:11434
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - opentron-net

  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
    networks:
      - opentron-net

volumes:
  postgres_data:

networks:
  opentron-net:
    driver: bridge
```

Start with:
```bash
docker-compose up -d
```

---

## Troubleshooting

### Problem: "Connection refused: localhost:5432"
**Solution:** Make sure PostgreSQL is running
```bash
# macOS
brew services start postgresql@15

# Linux
sudo systemctl start postgresql

# Docker
docker start opentron-postgres
```

### Problem: "FATAL: password authentication failed"
**Solution:** Check your environment variables
```bash
echo $POSTGRES_USER
echo $POSTGRES_PASSWORD
# Make sure they match the database credentials
```

### Problem: "Flyway: Unable to locate migration"
**Solution:** Ensure migration file is in correct location
```
backend/src/main/resources/db/migration/V1__Initial_Schema.sql
```

### Problem: "No database selected"
**Solution:** Create the database
```sql
CREATE DATABASE opentron;
```

### Problem: Build fails with "postgresql not found"
**Solution:** Update Maven dependencies
```bash
mvn clean install
# Or if using IDE, reload Maven project
```

---

## Monitoring & Logs

### View Logs
```bash
tail -f logs/opentron-backend.log
```

### Check Storage Metrics
```bash
curl http://localhost:8000/actuator/metrics
```

### Query Database Stats
```sql
-- Memory entries by agent
SELECT agent_name, COUNT(*) as count FROM agent_memory GROUP BY agent_name;

-- Traces by agent
SELECT agent, COUNT(*) as count FROM trace_logs GROUP BY agent;

-- Largest traces
SELECT agent, LENGTH(output) as output_size FROM trace_logs ORDER BY output_size DESC LIMIT 10;

-- Compression savings
SELECT 
    COUNT(*) as compressed_count,
    SUM(LENGTH(compressed_data)) / 1024.0 as compressed_kb
FROM trace_logs WHERE is_compressed = true;
```

---

## Next Steps

1. **Integrate with Agents Controller** - Add `StorageService` injection
2. **Add Scheduled Cleanup** - Create `CleanupScheduler` for retention policies
3. **Add Metrics** - Export storage metrics to Prometheus
4. **Migrate Existing Data** - Create data import from JSONL files (if needed)
5. **Set Up Backups** - PostgreSQL backup strategy
6. **Monitor Performance** - Add database connection monitoring

---

## Performance Tips

- **Connection Pool:** Max 20, Min 5 (adjust based on load)
- **Batch Inserts:** Enabled in Hibernate config (batch_size=20)
- **Compression:** Auto-enabled for traces > 1KB
- **Indexes:** Created on agent_name, timestamp, trace_hash
- **Cleanup:** Schedule archival of data older than 30 days

---

## Support

For issues or questions:
1. Check logs: `logs/opentron-backend.log`
2. Verify PostgreSQL: `psql -U opentron -d opentron -h localhost`
3. Check database tables: `\dt` in psql
4. Monitor metrics: `http://localhost:8000/actuator/metrics`

Good luck! 🚀
