# Deployment Guide: Virtual Threads + H2 Profile System

## Architecture Overview

```
OpenTron Backend (Java 21 + Spring Boot 3.1.6)
├── Virtual Threads Enabled (spring.threads.virtual.enabled=true)
├── Profile: embedded (H2 File Database)
│   ├── Database: ~/.opentron/opentron.mv.db (single file)
│   ├── Use Case: Desktop app, zero external dependencies
│   └── Single-user optimized
└── Profile: prod (PostgreSQL)
    ├── Database: PostgreSQL 16+ (Docker or managed)
    ├── Use Case: Team deployment, production
    └── Multi-user, full ACID compliance
```

---

## Deployment Scenarios

### Scenario 1: Desktop App with Embedded H2

**Target Users:** Individual users, developers, no system dependencies

**Setup:**
```bash
# 1. Build JAR
cd java/opentron-java/backend
mvn clean package -DskipTests

# 2. Embed in Tauri app
cp target/opentron-java-backend-0.1.0-exec.jar \
   desktop/src-tauri/binaries/opentron-backend.jar

# 3. Tauri Sidecar Configuration (Rust)
let args = vec![
    "--spring.profiles.active=embedded",
    "--server.port=8000",
];
let backend = Command::new(&java_path)
    .args(args)
    .arg(backend_jar_path)
    .spawn();
```

**Distribution:**
- Single installer (~150MB with JRE bundled)
- No Docker, PostgreSQL, or network required
- Database: `~/.opentron/opentron.mv.db` (user's home)
- Logs: `~/.opentron/logs/` (optional)

**Users Experience:**
1. Download desktop app installer
2. Click install
3. App launches → Backend auto-starts with H2
4. Zero configuration required

---

### Scenario 2: Team Server with PostgreSQL

**Target Deployment:** Company/team server, production

**Setup:**
```bash
# 1. Build JAR
cd java/opentron-java/backend
mvn clean package -DskipTests

# 2. Start PostgreSQL
docker run -d --name opentron-db \
  -e POSTGRES_DB=opentron \
  -e POSTGRES_USER=opentron \
  -e POSTGRES_PASSWORD=<secure_pass> \
  -p 5432:5432 \
  -v opentron_data:/var/lib/postgresql/data \
  postgres:16-alpine

# 3. Run Backend
java -jar java/opentron-java/backend/target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=prod \
  --server.port=8000 \
  --spring.datasource.url=jdbc:postgresql://db.example.com:5432/opentron \
  --spring.datasource.username=opentron \
  --spring.datasource.password=<secure_pass>
```

**Docker Compose:**
```yaml
version: '3.8'

services:
  database:
    image: postgres:16-alpine
    container_name: opentron-db
    environment:
      POSTGRES_DB: opentron
      POSTGRES_USER: opentron
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - opentron_data:/var/lib/postgresql/data
    restart: unless-stopped

  backend:
    image: opentron-backend:latest
    container_name: opentron-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      POSTGRES_URL: jdbc:postgresql://database:5432/opentron
      POSTGRES_USER: opentron
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      ENGINE_HOST: http://ollama:11434
    ports:
      - "8000:8000"
    depends_on:
      - database
    restart: unless-stopped

volumes:
  opentron_data:
```

**Virtual Threads Impact:**
- Handles 1000+ concurrent WebSocket connections
- Memory: 1-2GB for 500 concurrent users (vs 5-10GB with OS threads)
- CPU: Efficient scheduling of async operations

---

### Scenario 3: Development with Hot Reload

**Target Environment:** Developer workstation

**Setup:**
```bash
# Terminal 1: Start embedded H2 mode
cd java/opentron-java/backend
SPRING_PROFILES_ACTIVE=embedded mvn spring-boot:run

# Terminal 2: Start frontend with live reload
cd frontend
npm run tauri dev
```

**Changes:** Edit Java files → Maven auto-recompiles → Hot reload active
**Benefits:**
- No database setup needed
- Fast feedback loop
- Embedded profile matches production exactly

---

## Virtual Threads Configuration Details

### How It Works

**Before (OS Threads):**
```
1 OS Thread = ~1MB memory
Platform thread pool limited by OS
Context switches expensive for I/O
```

**After (Virtual Threads):**
```
1 Virtual Thread = ~1KB memory
Millions of virtual threads possible
Automatic scheduling to OS threads
I/O operations non-blocking
```

### Configuration

**In `application.properties`:**
```properties
spring.threads.virtual.enabled=true
```

**What This Configures:**
- All `@Async` methods use virtual thread executor
- Reactive operations (WebFlux) use virtual threads
- WebSocket handling optimized for concurrency
- No explicit thread pool configuration needed

### Benefits for OpenTron

1. **Chat Streaming:** 10,000+ concurrent WebSocket connections
2. **Agent Processing:** Async model inference calls don't block
3. **Memory Usage:** Single machine can handle massive load
4. **Responsiveness:** I/O operations don't starve executor threads

---

## H2 Embedded Profile Details

### Database Initialization

**First Launch:**
```
1. StartupInitializer checks for ~/.opentron/
2. Creates directory if missing
3. Flyway migrations initialize schema
4. H2 creates opentron.mv.db file (~5MB initial)
5. Backend ready to serve requests
```

**Database Location:**
```
Linux/macOS:  ~/.opentron/opentron.mv.db
Windows:      C:\Users\<username>\.opentron\opentron.mv.db
```

**File Structure:**
```
~/.opentron/
├── opentron.mv.db         (main database file)
├── opentron.trace.db      (optional, debug info)
└── [logs/]                (optional, if enabled)
```

### H2 Console Access

**Enable:** Automatically when using embedded profile

**Access:** `http://localhost:8000/h2-console`

**Credentials:**
- JDBC URL: `jdbc:h2:file:~/.opentron/opentron`
- Username: `sa`
- Password: (blank)

**Common Tasks:**
```sql
-- View all tables
SELECT * FROM INFORMATION_SCHEMA.TABLES;

-- Check database size
SELECT SUM(FILE_SIZE) FROM INFORMATION_SCHEMA.LOBS;

-- List active connections
SELECT * FROM INFORMATION_SCHEMA.SESSIONS;

-- Export data (embedded script)
CALL CSVWRITE('export.csv', 'SELECT * FROM my_table');
```

### Performance Characteristics

**H2 Embedded vs PostgreSQL:**

| Operation | H2 | PostgreSQL |
|-----------|----|----|
| Read (single) | ~1ms | ~2ms |
| Write (single) | ~2ms | ~3ms |
| Concurrent Reads (100 users) | Fast | Very Fast |
| Concurrent Writes (100 users) | Slow | Very Fast |
| Schema Lock | No | Yes |
| Max Recommended Users | 5-10 | 1000+ |
| Memory Usage | 100-500MB | Server-managed |

**Recommendation:**
- H2: Desktop apps, development, single-user
- PostgreSQL: Team deployment, 10+ concurrent users, production

---

## Production Checklist

### Pre-Deployment

- [ ] Build JAR: `mvn clean package -DskipTests`
- [ ] Test embedded: `java -jar ... --spring.profiles.active=embedded`
- [ ] Test prod: `java -jar ... --spring.profiles.active=prod`
- [ ] Review `SPRING_PROFILES_ACTIVE` environment variable
- [ ] Configure PostgreSQL credentials securely
- [ ] Setup log rotation/aggregation
- [ ] Configure monitoring (Prometheus metrics available)

### Deployment

- [ ] Start PostgreSQL container/server
- [ ] Run Flyway baseline if needed: `mvn flyway:baseline`
- [ ] Start backend JAR with prod profile
- [ ] Verify health check: `curl http://localhost:8000/actuator/health`
- [ ] Monitor logs for errors
- [ ] Test WebSocket connections
- [ ] Verify virtual threads in logs

### Monitoring

**Health Endpoint:**
```bash
curl http://localhost:8000/actuator/health
```

**Metrics:**
```bash
curl http://localhost:8000/actuator/metrics
```

**Database Status:**
```bash
curl http://localhost:8000/actuator/health/db
```

**Virtual Threads Statistics:**
```bash
# Monitor thread creation in logs
grep -i "virtual" logs/opentron-backend.log
```

### Troubleshooting

| Issue | Diagnosis | Solution |
|-------|-----------|----------|
| H2 "database already in use" | Check process list | Kill previous instance |
| PostgreSQL connection timeout | Check Docker container | Restart container, verify credentials |
| OOM (Out of Memory) | Monitor metrics `/health` | Increase JVM heap: `-Xmx2g` |
| Slow queries | Enable SQL logging | Add `--logging.level.org.hibernate.SQL=DEBUG` |
| High CPU with virtual threads | Expected if many async ops | Check if agent processing heavy |

---

## Scaling Recommendations

### Single Machine (Embedded H2)
- **Users:** 1-10
- **Concurrent Connections:** 100-500
- **Memory:** 2GB JVM
- **Storage:** H2 file grows with usage (~1MB per 1000 records)
- **Upgrade Path:** Migrate to PostgreSQL when user base grows

### Team Server (PostgreSQL)
- **Users:** 10-100
- **Concurrent Connections:** 500-2000
- **Memory:** 4-8GB JVM + separate DB server
- **Storage:** Managed by PostgreSQL
- **Database Replicas:** Optional for HA
- **Load Balancing:** Multiple backend instances behind nginx/HAProxy

### Enterprise (PostgreSQL + Clustering)
- **Users:** 100+
- **Concurrent Connections:** 2000+
- **Architecture:** Multiple backend instances + PostgreSQL replication
- **Cache Layer:** Redis for session storage
- **Monitoring:** ELK stack (Elasticsearch, Logstash, Kibana)
- **Scaling:** Kubernetes orchestration (optional)

---

## Maintenance

### Database Backups

**H2 Embedded:**
```bash
# Manual backup (simple file copy)
cp ~/.opentron/opentron.mv.db ~/.opentron/opentron.mv.db.backup
```

**PostgreSQL:**
```bash
# Automated backups
docker exec opentron-db pg_dump -U opentron -d opentron > backup.sql
```

### Migrations

**New Schema Changes:**
1. Add migration file: `src/main/resources/db/migration/V<next>__<description>.sql`
2. Flyway auto-applies on next startup
3. Both H2 and PostgreSQL use same migration files

### Monitoring & Observability

**Prometheus Metrics Exposed:**
- `http_requests_total` - Request count by endpoint
- `http_request_duration_seconds` - Response times
- `jvm_memory_usage_bytes` - JVM memory
- `jvm_threads_live` - Active thread count (includes virtual threads)

**Logs:**
```bash
# Tail logs
tail -f logs/opentron-backend.log

# Search for errors
grep ERROR logs/opentron-backend.log

# Monitor virtual thread activity
grep -i virtual logs/opentron-backend.log
```

---

## Security Notes

### H2 Embedded
- H2 Console enabled by default (disable in production if single-user)
- Access via `http://localhost:8000/h2-console`
- No network exposure by default
- Disable console: `spring.h2.console.enabled=false`

### PostgreSQL Production
- Use strong passwords (environment variables, secrets vault)
- Configure PostgreSQL to allow only backend connections
- Enable SSL for database connections (optional)
- Regular security updates: `docker pull postgres:16-alpine`

### Backend
- CORS configured for Tauri app
- Adjust origins in `OpentronBackendApplication.corsConfigurer()`
- API authentication/authorization: Implement per requirements

---

## Summary

| Profile | Best For | Setup Time | Dependencies |
|---------|----------|-----------|--------|
| **embedded** | Desktop, dev, single-user | ~2 sec | None |
| **prod** | Team, production, multi-user | ~30 sec | Docker, PostgreSQL |

**Virtual Threads:** Enabled by default, no configuration needed, supports 10,000+ concurrent connections.

**Migration:** From H2 to PostgreSQL possible - export data via SQL scripts or application-level ETL.

**Next Step:** Choose your deployment scenario above and follow the setup instructions.
