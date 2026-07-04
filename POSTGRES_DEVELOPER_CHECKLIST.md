# PostgreSQL Database Integration - Developer Checklist

## ✅ Setup Checklist (Before You Start)

### PostgreSQL Setup
- [ ] Docker running on your machine
- [ ] PostgreSQL container created and running
  ```bash
  docker run -d --restart always --name opentron-postgres \
    -e POSTGRES_DB=opentron \
    -e POSTGRES_USER=opentron \
    -e POSTGRES_PASSWORD=opentron_secure_password \
    -p 5432:5432 \
    -v postgres_data:/var/lib/postgresql/data \
    postgres:16-alpine
  ```
- [ ] Verify database is accessible: `psql -U opentron -d opentron -h localhost`

### Environment Variables
- [ ] Set POSTGRES_URL: `jdbc:postgresql://localhost:5432/opentron`
- [ ] Set POSTGRES_USER: `opentron`
- [ ] Set POSTGRES_PASSWORD: `opentron_secure_password`
- [ ] Set ENGINE_HOST: `http://localhost:11434` (Ollama)

### Backend Build
- [ ] Run: `mvn clean package -DskipTests`
- [ ] Verify build succeeds (BUILD SUCCESS message)
- [ ] Check for no compile errors

### Database Verification
- [ ] Start backend: `mvn spring-boot:run`
- [ ] Check logs for "Flyway: Executing migration V1__Initial_Schema.sql"
- [ ] Check logs for "Successfully created tables"
- [ ] Verify tables in psql: `\dt`
- [ ] Check migration history: `SELECT * FROM flyway_schema_history;`

---

## 🔧 Integration Steps (By Component)

### Step 1: Integrate with AgentsController

**File:** `backend/src/main/java/org/opentron/backend/controllers/AgentsController.java`

1. Add import:
   ```java
   import org.opentron.backend.storage.service.StorageService;
   ```

2. Add field:
   ```java
   @Autowired
   private StorageService storageService;
   ```

3. In `executeAgent()` method, add after execution:
   ```java
   return result.doOnNext(response -> {
       try {
           String input = request.get("input").toString();
           String output = response.getBody().toString();
           storageService.saveTrace(id, input, output, durationMs);
       } catch (Exception e) {
           log.warn("Failed to save trace: " + e.getMessage());
       }
   });
   ```

**Test:**
- [ ] Execute an agent
- [ ] Check logs for "[StorageService] Saved trace for agent"
- [ ] Query database: `SELECT * FROM trace_logs ORDER BY id DESC LIMIT 1;`

---

### Step 2: Integrate with MemoryService

**File:** `backend/src/main/java/org/opentron/backend/memory/MemoryService.java`

1. Add import:
   ```java
   import org.opentron.backend.storage.service.StorageService;
   import org.opentron.backend.storage.entities.AgentMemory;
   ```

2. Add field:
   ```java
   @Autowired
   private StorageService storageService;
   ```

3. Add method:
   ```java
   public void storeMemory(String agentName, String trace, String summary) {
       try {
           AgentMemory memory = storageService.saveAgentMemory(agentName, trace, summary);
           log.info("Stored memory for " + agentName + " (ID: " + memory.getId() + ")");
       } catch (Exception e) {
           log.error("Error storing memory: " + e.getMessage());
       }
   }
   ```

4. Add method to retrieve:
   ```java
   public List<AgentMemory> retrieveMemory(String agentName, int limit) {
       return storageService.loadAgentMemory(agentName, limit);
   }
   ```

**Test:**
- [ ] Store a memory
- [ ] Check logs for "[StorageService] Saved memory for"
- [ ] Query: `SELECT * FROM agent_memory ORDER BY id DESC LIMIT 1;`
- [ ] Retrieve memory and verify it returns correct data

---

### Step 3: Add Scheduled Cleanup

**File:** `backend/src/main/java/org/opentron/backend/storage/scheduler/CleanupScheduler.java` (create new)

Create this file with:
```java
package org.opentron.backend.storage.scheduler;

import org.opentron.backend.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CleanupScheduler {
    
    @Autowired
    private StorageService storageService;
    
    // Daily cleanup at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyCleanup() {
        log.info("[CleanupScheduler] Starting daily cleanup...");
        
        // Archive traces older than 30 days
        storageService.archiveOldTraces(30);
        
        // Archive memory older than 60 days
        storageService.archiveOldMemory(60);
        
        log.info("[CleanupScheduler] Daily cleanup completed");
    }
    
    // Weekly statistics
    @Scheduled(cron = "0 0 0 ? * MON")
    public void weeklyStats() {
        StorageService.StorageStats stats = storageService.getStorageStats();
        log.info("[CleanupScheduler] Weekly stats: " + stats.toString());
    }
}
```

**Test:**
- [ ] Backend starts without errors
- [ ] Check logs for scheduler startup message
- [ ] Run manual test: `storageService.archiveOldTraces(0);` (archive all)

---

### Step 4: Add Monitoring Metrics

**File:** `backend/src/main/java/org/opentron/backend/storage/metrics/StorageMetrics.java` (create new)

Create this file with:
```java
package org.opentron.backend.storage.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class StorageMetrics {
    private final MeterRegistry meterRegistry;
    
    public StorageMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordTraceSave(String agent, long durationMs) {
        Timer.builder("storage.trace.save")
            .tag("agent", agent)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
```

**Test:**
- [ ] Access metrics: `curl http://localhost:8000/actuator/metrics`
- [ ] Should show storage.trace.save metrics

---

## 🧪 Testing Checklist

### Unit Test: Compression
```java
@Test
public void testCompression() {
    String data = "X".repeat(10000);
    byte[] compressed = compressionService.compress(data.getBytes());
    byte[] decompressed = compressionService.decompress(compressed);
    assertEquals(data, new String(decompressed));
    assertTrue(compressed.length < data.length());
}
```
- [ ] Compression reduces size for large data
- [ ] Decompression recovers original data

### Unit Test: Deduplication
```java
@Test
public void testDeduplication() {
    // Save same trace twice
    AgentMemory mem1 = storageService.saveAgentMemory("agent1", "same", "summary");
    AgentMemory mem2 = storageService.saveAgentMemory("agent1", "same", "summary");
    
    // Should return same object (deduplicated)
    assertEquals(mem1.getId(), mem2.getId());
}
```
- [ ] Duplicate traces are not duplicated in database
- [ ] SHA256 hashing works correctly

### Integration Test: Save & Load
```java
@Test
public void testSaveAndLoad() {
    // Save
    storageService.saveTrace("agent1", "input", "output".repeat(1000), 100);
    
    // Load
    List<TraceLog> traces = storageService.loadTraces("agent1", 10);
    
    // Verify
    assertEquals(1, traces.size());
    assertTrue(traces.get(0).getIsCompressed());  // Should be compressed
    assertNotNull(traces.get(0).getCompressedData());
}
```
- [ ] Traces are saved successfully
- [ ] Large outputs are compressed
- [ ] Traces can be loaded and decompressed

### Integration Test: Performance
```java
@Test
public void testPerformance() {
    long start = System.currentTimeMillis();
    
    // Save 1000 traces
    for (int i = 0; i < 1000; i++) {
        storageService.saveTrace("agent1", "input" + i, "output" + i, 100);
    }
    
    long duration = System.currentTimeMillis() - start;
    System.out.println("Saved 1000 traces in " + duration + "ms");
    
    // Should complete in < 5 seconds
    assertTrue(duration < 5000);
}
```
- [ ] 1000 traces saved in under 5 seconds
- [ ] Connection pooling is working

---

## 📊 Manual Database Tests

### Test 1: Verify Tables Created
```bash
psql -U opentron -d opentron -h localhost
\dt

# Should see:
#  public | agent_memory
#  public | trace_logs
#  public | skills
#  public | document_index
#  public | agent_sessions
```
- [ ] All 6 main tables exist

### Test 2: Test Compression
```sql
-- After saving a large trace
SELECT 
    LENGTH(output) as raw_size,
    LENGTH(compressed_data) as compressed_size,
    ROUND(LENGTH(compressed_data)::numeric / LENGTH(output) * 100, 1) as ratio_percent,
    is_compressed
FROM trace_logs 
WHERE agent = 'agent1' AND is_compressed = true
LIMIT 1;
```
- [ ] Compression ratio is 40-70%
- [ ] is_compressed flag is true

### Test 3: Test Deduplication
```sql
-- Check for duplicates (should be zero)
SELECT agent_name, trace_hash, COUNT(*) as count
FROM agent_memory
GROUP BY agent_name, trace_hash
HAVING COUNT(*) > 1;

-- Should return no rows
```
- [ ] No duplicate traces in database

### Test 4: Test Indexing
```sql
-- Check that indexes are used
EXPLAIN ANALYZE
SELECT * FROM agent_memory 
WHERE agent_name = 'agent1' 
ORDER BY timestamp DESC 
LIMIT 10;

-- Should see "Index Scan" in output
```
- [ ] Queries use indexes
- [ ] Query time is <10ms for 100K records

### Test 5: Test Statistics Views
```sql
SELECT * FROM agent_statistics;
SELECT * FROM trace_statistics;
SELECT * FROM compression_analysis;
```
- [ ] Views show meaningful statistics
- [ ] Aggregation is correct

---

## 🚀 Production Readiness Checklist

### Backups
- [ ] PostgreSQL backup strategy documented
- [ ] Test backup: `pg_dump -U opentron -d opentron > backup.sql`
- [ ] Test restore from backup
- [ ] Schedule automated backups

### Monitoring
- [ ] Health check: `curl http://localhost:8000/v1/health`
- [ ] Metrics exposed: `curl http://localhost:8000/actuator/metrics`
- [ ] Prometheus scraping configured
- [ ] Alert thresholds set

### Performance
- [ ] Connection pool tuned (currently 20 max, 5 min)
- [ ] Batch size set to 20
- [ ] Load test: 1000 traces/second
- [ ] Storage size under control

### Documentation
- [ ] Runbook created
- [ ] Team trained on operations
- [ ] Disaster recovery procedure documented
- [ ] Common issues documented

### Security
- [ ] Passwords managed via environment variables
- [ ] No credentials in code
- [ ] Database has user with limited permissions
- [ ] Backup encryption enabled (if cloud)

---

## 📋 Sign-Off Checklist

### Development
- [ ] Code reviewed by peer
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] No lint errors
- [ ] Documentation updated

### QA
- [ ] Manual testing completed
- [ ] Performance benchmarks met
- [ ] Database integrity verified
- [ ] Failover tested

### Operations
- [ ] Runbook created
- [ ] Monitoring configured
- [ ] Backup procedure verified
- [ ] On-call runbook created

### Go-Live
- [ ] All checklists completed
- [ ] Stakeholders approved
- [ ] Rollback procedure ready
- [ ] Team standing by

---

## 📞 Quick Reference

### Common Commands

**Start PostgreSQL:**
```bash
docker start opentron-postgres
```

**View logs:**
```bash
docker logs -f opentron-postgres
```

**Connect to database:**
```bash
psql -U opentron -d opentron -h localhost
```

**Backup database:**
```bash
pg_dump -U opentron -d opentron > backup.sql
```

**Restore database:**
```bash
psql -U opentron -d opentron < backup.sql
```

**List tables:**
```sql
\dt
```

**Count rows:**
```sql
SELECT COUNT(*) FROM agent_memory;
SELECT COUNT(*) FROM trace_logs;
```

**Check indexes:**
```sql
SELECT schemaname, tablename, indexname FROM pg_indexes 
WHERE schemaname = 'public';
```

---

## 🎯 Success Criteria

✅ PostgreSQL Integration Complete When:
1. [ ] All tables created via Flyway migration
2. [ ] StorageService successfully saves traces
3. [ ] Compression working (>1KB traces compressed)
4. [ ] Deduplication working (no duplicate hashes)
5. [ ] Queries return results in <10ms
6. [ ] 1000+ traces saved per second
7. [ ] Storage size 65-85% smaller than files
8. [ ] Scheduled cleanup runs daily
9. [ ] Monitoring shows metrics
10. [ ] Team trained and confident

---

**Total Estimated Time:** 4-6 hours for full integration

Good luck! 🚀
