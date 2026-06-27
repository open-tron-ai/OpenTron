# Python-to-Java Backend Migration & Cutover Plan

## Overview

This document outlines the migration strategy from OpenTron's Python backend to the new Java Spring Boot backend. The goal is to replace the Python server entirely with Java while maintaining full API compatibility and zero downtime.

## Migration Timeline

| Phase | Duration | Goal |
|-------|----------|------|
| **Phase 1: Validation** | 1-2 days | Run integration tests, validate endpoint parity |
| **Phase 2: Staging** | 2-3 days | Deploy Java backend to staging, run smoke tests, monitor metrics |
| **Phase 3: Canary Deployment** | 1-2 days | Gradual traffic shift (10% → 25% → 50% → 100%) to Java backend |
| **Phase 4: Full Cutover** | 1 hour | Complete traffic migration, decommission Python backend |
| **Phase 5: Monitoring** | 7+ days | Monitor error rates, latency, resource usage; ensure stability |

## Pre-Cutover Checklist

### Backend Implementation ✅
- [x] Implement `/v1/models` endpoint (GET proxy to engine)
- [x] Implement `/v1/chat/completions` endpoint (POST with stream support)
- [x] Implement `/v1/chat/stream` endpoint (WebSocket → HTTP SSE adapter)
- [x] Add header preservation (skip Content-Length, preserve others)
- [x] Add proper error handling (502 Bad Gateway for engine failures)
- [x] Add resilience: retry (non-streaming) + timeout
- [x] Add CORS configuration for frontend origins
- [x] Configure WebClient with connection pooling (200 max, 60s timeout)
- [x] Add structured logging (Logback) and metrics (Prometheus/Micrometer)

### Integration Tests ✅
- [x] Run `EngineStubIntegrationTest` against embedded engine stub
  - testModelsProxy: GET /v1/models → JSON with model list
  - testChatSSEStream: POST /v1/chat/completions (stream=true) → SSE chunks
  - testWebSocketRelay: WS /v1/chat/stream → JSON chunks from SSE

### Docker & Deployment ✅
- [x] Update Dockerfile: Node (frontend) + Maven (Java backend) + Runtime (JRE)
- [x] Update docker-compose.yml: Java backend + Ollama engine
- [x] Configure environment variables (ENGINE_HOST, ENGINE_APIKEY)
- [x] Create deployment README with setup instructions

### Performance & Load Testing (TODO - Optional)
- [ ] Load test: 100+ concurrent connections to /v1/chat/completions (stream=true)
- [ ] Load test: 50+ concurrent WebSocket connections to /v1/chat/stream
- [ ] Baseline latency: P50, P95, P99 response times
- [ ] Resource usage: CPU, memory under load (compare Python vs Java)

### Frontend Compatibility (TODO)
- [ ] Verify frontend can connect to Java backend
- [ ] Test all chat features: SSE streaming, WebSocket streaming, model selection
- [ ] Test error scenarios: engine down, timeouts, invalid requests

## Phase 1: Validation (Local & Staging)

### Step 1: Build and Test Java Backend

```bash
cd java/opentron-java/backend
mvn clean package
mvn test  # Run integration tests
```

Expected results:
- All 3 integration tests pass
- No compilation errors
- Build artifact: `target/opentron-java-backend-0.1.0.jar`

### Step 2: Run Docker Build

```bash
cd ../..  # Back to OpenTron root
docker build -f deploy/docker/Dockerfile -t opentron-backend:java-latest .
```

Expected:
- Build succeeds
- Image size: ~400MB (Java 17 JRE + Spring Boot + JAR)

### Step 3: Deploy to Staging

```bash
cd deploy/docker
cp .env.example .env
# Edit .env: set ENGINE_APIKEY and ENGINE_HOST (point to staging Ollama)
docker compose up -d
```

Monitor logs:
```bash
docker compose logs -f opentron-backend
```

Expected startup logs:
```
[OpentronBackendApplication] engine.host=http://ollama:11434 apiKey=<set>
[OpentronBackendApplication] Started OpentronBackendApplication in X.XXs
```

### Step 4: Smoke Tests

**Test 1: Models endpoint**
```bash
API_KEY=$(cat .env | grep ENGINE_APIKEY | cut -d= -f2)
curl -H "Authorization: Bearer $API_KEY" http://localhost:8000/v1/models | jq .
# Expected: {"data":[{"id":"llama2"},...]}
```

**Test 2: Chat completions (non-streaming)**
```bash
curl -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2",
    "messages": [{"role": "user", "content": "Hello"}],
    "stream": false
  }' \
  http://localhost:8000/v1/chat/completions | jq .
# Expected: {"id":"...", "object":"chat.completion", "choices":[...]}
```

**Test 3: Chat completions (streaming)**
```bash
curl -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2",
    "messages": [{"role": "user", "content": "Hello"}],
    "stream": true
  }' \
  http://localhost:8000/v1/chat/completions
# Expected: SSE chunks with "data: {json}\n\n"
```

**Test 4: WebSocket streaming**
```bash
wscat -c ws://localhost:8000/v1/chat/stream
# Send: {"model":"llama2","messages":[{"role":"user","content":"Hello"}]}
# Expected: JSON chunks {"choices":[{"delta":{"content":"..."}}}
```

### Step 5: Verify Metrics & Logging

**Prometheus metrics** (if enabled):
```bash
curl http://localhost:8000/actuator/metrics
# Expected: Connection pool metrics, HTTP request metrics
```

**Logs**:
```bash
docker compose logs opentron-backend | grep -E "error|ERROR|warn|WARN"
# Expected: No critical errors
```

## Phase 2: Staging Deployment

### Pre-Staging Checklist
- [ ] Staging environment has same Ollama models as production
- [ ] API key is different from production (staging-specific)
- [ ] Frontend can point to staging Java backend (DNS/hosts)
- [ ] Monitoring/alerting configured for staging

### Staging Deployment Steps

1. **Deploy Java backend to staging cluster**
   ```bash
   docker compose -f docker-compose.yml up -d
   ```

2. **Configure frontend to use staging Java backend**
   - Update `.env` or environment variable for backend URL
   - Rebuild frontend SPA

3. **Run end-to-end tests**
   - Test chat with streaming
   - Test WebSocket connections
   - Test error scenarios (engine down, invalid models)
   - Monitor latency, error rates

4. **Monitor for 2-3 days**
   - Watch error logs, metrics, latency
   - Compare performance (latency, resource usage) vs Python backend
   - Get stakeholder approval to proceed

## Phase 3: Canary Deployment (Production)

### Prerequisites
- [ ] Production monitoring (Prometheus, Grafana) ready
- [ ] Alert thresholds defined:
  - Error rate > 1% triggers page
  - P99 latency > 10s triggers warning
  - WebSocket connection drops > 5% triggers warning
- [ ] Rollback procedure tested and documented

### Canary Strategy: Gradual Traffic Shift

Use a reverse proxy (nginx, HAProxy, AWS ALB) to split traffic:

**Initial state (Java backend running alongside Python)**
```
Clients → LB (round-robin or weighted)
         ├─ 10% to Java (opentron-backend:8000)
         └─ 90% to Python (opentron-server:8000)
```

**Canary phases (over 1-2 hours)**
1. **Canary 1**: 10% Java, 90% Python (30 min, monitor)
2. **Canary 2**: 25% Java, 75% Python (30 min, monitor)
3. **Canary 3**: 50% Java, 50% Python (30 min, monitor)
4. **Full Cutover**: 100% Java, 0% Python (1 min)

### Monitoring During Canary

Key metrics to track:
- **Error rate** by backend (Prometheus: `http_requests_total{status="5xx"}`)
- **Latency** by backend (Prometheus: `http_request_duration_seconds`)
- **WebSocket connection success rate** (check logs for "new websocket connection")
- **Backend resource usage** (CPU, memory, connection pool size)

**Decision points at each phase:**
- Error rate < 0.5%? Continue to next phase
- P99 latency within 10% of Python? Continue
- Any websocket drops > 5%? Rollback immediately

### Rollback Procedure

If any metric threshold is exceeded:

```bash
# Option 1: Reverse traffic split
LB config: 100% Python, 0% Java

# Option 2: Full rollback (if systemic issue)
docker kill opentron-backend-java
docker start opentron-backend-python
```

**RTO**: < 1 min (reverse proxy update + DNS TTL)

## Phase 4: Full Cutover (Hour 0)

Once Phase 3 reaches 100% Java with all metrics green:

1. **Final validation**
   - Check all endpoint response times, error rates
   - Sample 100 requests, verify 100% success
   - Test WebSocket with 50+ simultaneous clients

2. **Execute cutover (during planned maintenance window)**
   ```bash
   # 1. Remove Python backend from load balancer
   #    (frontend continues to work, requests fail over to Java)
   
   # 2. Wait for in-flight requests to complete (5 min)
   
   # 3. Decommission Python services
   docker compose down -v  # Remove Python backend + volumes
   
   # 4. Keep Java backend running
   docker compose up -d  # Start/keep Java backend
   ```

3. **Verify cutover (minutes 0-15)**
   - Monitor error logs: should be zero critical errors
   - Sample requests to all endpoints
   - Monitor frontend user experience reports

4. **Enable full traffic monitoring (minutes 15+)**
   - Spike detection for latency increases
   - Spike detection for error rates
   - Daily review of metrics vs baseline

## Phase 5: Post-Cutover Monitoring (7+ Days)

### Success Criteria
- [ ] Error rate stable at < 0.1% (same as Python baseline)
- [ ] P95 latency within 5% of Python baseline
- [ ] P99 latency within 10% of Python baseline
- [ ] WebSocket success rate > 99%
- [ ] No memory leaks (monitor heap usage over 7 days)
- [ ] No connection pool exhaustion issues
- [ ] Frontend users report no issues (via feedback/analytics)

### Daily Monitoring Tasks
1. Review error logs: search for "ERROR", "FATAL", timeouts
2. Check metrics dashboard:
   - Connection pool utilization (target: < 70%)
   - Request latency percentiles (P50, P95, P99)
   - Error rate by endpoint
3. Check resource usage: CPU, memory, disk
4. Review Prometheus alerts: any firing?

### Long-Term Monitoring
- Weekly performance review: latency trends, error rates
- Monthly resource audit: optimize connection pool, timeout settings
- Quarterly load testing: ensure scalability

### Decommissioning Python Backend
After 30 days of successful Java backend operation:
1. Archive Python codebase (git tag `python-backend-final`)
2. Remove Python deployment files:
   - `deploy/docker/Dockerfile.python` (if created)
   - Python-specific deployment docs
3. Update README: Java backend is now the standard

## Verification Checklist

### Before Cutover
- [x] Integration tests pass (EngineStubIntegrationTest)
- [ ] Load tests pass (100+ concurrent connections)
- [ ] Frontend E2E tests pass (chat, streaming, WebSocket)
- [ ] Staging deployment stable for 2+ days
- [ ] Canary deployment reaches 100% with no errors

### After Cutover
- [ ] Error rate < 0.1%
- [ ] P95 latency within 5% of baseline
- [ ] All 3 endpoints responding correctly
- [ ] WebSocket connections stable (0 unexpected closes)
- [ ] Memory usage stable (no leaks)
- [ ] Users report normal experience

## Rollback Decision Tree

```
During Canary (Phase 3):
  Error rate > 1%?
    → YES: Rollback immediately (revert to Python)
    → NO: Continue
  
  P99 latency > 150% of Python?
    → YES: Rollback immediately
    → NO: Continue
  
  WebSocket drops > 5%?
    → YES: Rollback immediately
    → NO: Continue
  
  All thresholds OK for 30 min?
    → YES: Proceed to next canary phase
    → NO: Investigate, may require code fix or rollback

Post-Cutover (Phase 4-5):
  Critical error surge within 1 hour?
    → YES: Trigger full rollback procedure
    → NO: Continue monitoring
  
  Memory leak detected (heap growth > 50% over 24h)?
    → YES: Pause new requests, investigate, may rollback
    → NO: Continue
  
  7 days of stable operation?
    → YES: Migration complete, proceed with decommissioning Python
    → NO: Extend monitoring period
```

## Known Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|-----------|
| Java backend crashes under load | 100% downtime | Low | Load test before cutover; monitor heap/GC |
| WebSocket implementation broken | WebSocket users blocked | Low | Canary test with 50+ concurrent WS clients |
| SSE streaming broken (network issue) | Streaming broken | Medium | Test with various network conditions; monitor errors |
| Memory leak in JVM | Degradation over time | Low | Monitor heap growth; add periodic restart if needed |
| Connection pool exhaustion | Requests timeout | Low | Monitor pool utilization; adjust max connections if needed |
| Engine (Ollama) becomes bottleneck | Cascading timeouts | Medium | Scale Ollama independently; use backend retries |
| Certificate/Auth issues | All requests fail | Medium | Pre-test auth (engine.apiKey) before cutover |

## Appendix: Key Metrics Dashboard

Recommended Prometheus queries for monitoring dashboard:

```promql
# Error rate (%)
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100

# P95 request latency
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# WebSocket connections
up{instance="opentron-backend:8000"}

# Connection pool utilization
opentron_connection_pool_size / opentron_connection_pool_max

# JVM heap usage (%)
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

## Questions & Escalation

- **Performance concerns**: Escalate to DevOps, review load test results
- **API compatibility issues**: Escalate to backend team, may require code changes
- **Frontend issues**: Escalate to frontend team, may need environment config changes
- **Urgent rollback decision**: On-call engineer authorized; notify team lead
