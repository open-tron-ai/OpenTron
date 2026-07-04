# Cost Comparison Real-Time Update Fix

## Problem
- Chat shows "1 request" but no tokens recorded
- Cost Comparison dashboard shows "awaiting first inference..." instead of populated data
- Savings data not updating even after running inference

## Root Causes

### 1. OllamaCliService: Token Counts Not Extracted
**File:** `backend/src/main/java/org/opentron/backend/util/OllamaCliService.java`

Ollama's `/api/generate` response includes:
- `prompt_eval_count` — number of prompt tokens
- `eval_count` — number of completion tokens

These were being **discarded** instead of included in the OpenAI-format response.

**Fix:** Extract and include usage data:
```java
// Extract token counts from Ollama response
long promptTokens = 0;
long completionTokens = 0;
if (ollamaResp.containsKey("prompt_eval_count")) {
    promptTokens = ((Number) ollamaResp.get("prompt_eval_count")).longValue();
}
if (ollamaResp.containsKey("eval_count")) {
    completionTokens = ((Number) ollamaResp.get("eval_count")).longValue();
}
long totalTokens = promptTokens + completionTokens;

// Add OpenAI-format usage block
Map<String, Object> usage = new LinkedHashMap<>();
usage.put("prompt_tokens", promptTokens);
usage.put("completion_tokens", completionTokens);
usage.put("total_tokens", totalTokens);
result.put("usage", usage);
```

### 2. ChatController: Token Usage Not Recorded to Telemetry
**File:** `backend/src/main/java/org/opentron/backend/controllers/ChatController.java`

The controller was calling `telemetryService.recordRequest()` but never calling `telemetryService.addTokens()`. Token usage from responses was not being extracted.

**Fix:** Extract usage from both:
- **Ollama CLI responses** (in `handleOllamaCliChat`): Extract from `result.get("usage")`
- **Proxied responses** (in `buildResponseMono`): Parse JSON response body to extract `usage.total_tokens`

```java
// Record token usage if available
Map<String, Object> usage = (Map<String, Object>) result.get("usage");
if (usage != null) {
    Object totalObj = usage.get("total_tokens");
    if (totalObj != null) {
        long tokens = ((Number) totalObj).longValue();
        if (ts != null) ts.addTokens(tokens);
    }
}
```

### 3. SavingsController: Using Hardcoded Data
**File:** `backend/src/main/java/org/opentron/backend/controllers/SavingsController.java`

Response was completely static with `total_calls: 42` and `total_tokens: 13770` hardcoded.

**Fix:** Inject `TelemetryService` and read live metrics:
```java
private final TelemetryService telemetryService;

public SavingsController(TelemetryService telemetryService) {
    this.telemetryService = telemetryService;
}

// In savings() endpoint:
long totalCalls = telemetryService.getTotalRequests();
long totalTokens = telemetryService.getTotalTokens();
```

## Files Modified

1. **OllamaCliService.java** — Extract token counts from Ollama response
2. **ChatController.java** — Record tokens in both CLI and proxied flows
3. **SavingsController.java** — Use TelemetryService for live metrics

## Expected Behavior After Fix

1. **First inference run:**
   - Tokens are extracted from Ollama response (e.g., `prompt_tokens: 50, completion_tokens: 120`)
   - `ChatController` calls `telemetryService.addTokens(170)`
   - Frontend receives `usage` in response

2. **Frontend updates:**
   - `InputArea.tsx` captures `usage` from stream event
   - Calls `store.incrementSavings(usage)` after stream completes
   - Calls `fetchSavings()` to refresh dashboard

3. **Dashboard updates:**
   - Cost Comparison shows actual token counts
   - Local cost calculated: `tokens * 1e-6` (Qwen 32B estimate)
   - Cloud provider costs calculated dynamically
   - Data refreshes every 30 seconds from `/v1/savings` poll

## Deployment Steps

1. Rebuild Java backend:
   ```bash
   cd java/opentron-java
   mvn clean package -DskipTests
   ```

2. Restart backend service

3. Clear browser cache and reload frontend

4. Run a chat inference — tokens should now update in real time

## Verification

After running one inference:
- Chat sidebar should show token count in message
- Dashboard Cost Comparison should display savings numbers instead of "awaiting first inference"
- `/v1/savings` endpoint should return actual data, not hardcoded values
- Cost comparison should update every 30 seconds as new inferences run
