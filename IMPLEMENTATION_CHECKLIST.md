# Cost Comparison Fix — Implementation Checklist

## Files Modified ✓

- [x] **OllamaCliService.java** — Now extracts `prompt_eval_count` and `eval_count` from Ollama response and includes them in the OpenAI-compatible `usage` block

- [x] **ChatController.java** — Now records token usage to `TelemetryService.addTokens()` for both:
  - Ollama CLI completions (via `handleOllamaCliChat`)
  - Proxied cloud API responses (via `buildResponseMono`)

- [x] **SavingsController.java** — Now injects `TelemetryService` and reads live metrics instead of returning hardcoded sample data

## Data Flow

### Before (Broken)
```
User sends message
  ↓
OllamaCliService.chatCompletion()
  → Ollama returns: {response: "...", prompt_eval_count: 50, eval_count: 120}
  → Response is stripped, usage discarded ✗
  ↓
ChatController receives response
  → No usage data to record ✗
  ↓
Frontend receives: {choices: [...], usage: null}
  → No tokens recorded ✗
  ↓
SavingsController.savings()
  → Returns hardcoded data (total_calls: 42, total_tokens: 13770) ✗
```

### After (Fixed)
```
User sends message
  ↓
OllamaCliService.chatCompletion()
  → Ollama returns: {response: "...", prompt_eval_count: 50, eval_count: 120}
  → Extract: promptTokens=50, completionTokens=120, totalTokens=170 ✓
  → Include in response: {choices: [...], usage: {prompt_tokens: 50, completion_tokens: 120, total_tokens: 170}} ✓
  ↓
ChatController.handleOllamaCliChat()
  → Extract usage from result: {usage: {total_tokens: 170}}
  → Call: telemetryService.addTokens(170) ✓
  ↓
Frontend receives: {choices: [...], usage: {total_tokens: 170}}
  → Parse usage, call store.incrementSavings({total_tokens: 170}) ✓
  ↓
InputArea.tsx calls fetchSavings()
  ↓
SavingsController.savings()
  → Reads: totalCalls = telemetryService.getTotalRequests() = 1 ✓
  → Reads: totalTokens = telemetryService.getTotalTokens() = 170 ✓
  → Calculates: localCost = 170 * 1e-6 = 0.00017 USD ✓
  → Calculates cloud costs based on actual tokens ✓
  → Returns: {total_tokens: 170, local_cost: 0.00017, per_provider: [...]} ✓
  ↓
Frontend updates Cost Comparison dashboard
  → Shows actual token counts and savings ✓
```

## Testing Steps

1. **Rebuild Java backend:**
   ```bash
   cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java
   mvn clean package -DskipTests
   ```
   Expected: `BUILD SUCCESS`

2. **Restart backend service** (if running as a service or manually restart the running instance)

3. **Clear browser cache:**
   - Chrome DevTools → Application → Clear site data
   - Or: Hard refresh (Ctrl+Shift+R)

4. **Send a test message in chat:**
   - Type a simple question (e.g., "Hello")
   - Wait for response

5. **Verify tokens are recorded:**
   - Check browser DevTools Network tab → find `/v1/savings` request
   - Inspect response JSON:
     ```json
     {
       "total_calls": 1,
       "total_tokens": 170,     ← Should be > 0
       "local_cost": 0.00017,   ← Should be > 0
       "per_provider": [...]    ← Should have cloud costs
     }
     ```

6. **Verify dashboard updates:**
   - Navigate to Dashboard → Cost Comparison
   - Should show:
     - Local cost: `$0.0002` (or similar)
     - Cloud comparisons with non-zero values
     - NOT "awaiting first inference..."

7. **Send more messages:**
   - Each new inference should increase token count
   - Costs should update in real time

## Debugging if Still Not Working

### Issue: Dashboard still shows "awaiting first inference..."

**Check 1:** Are tokens being extracted from Ollama?
```bash
# Look in backend logs for:
# [OllamaCliService] Response: XXX chars, tokens: 170
```
- If missing, Ollama isn't returning tokens or extraction is failing
- Verify Ollama is running: `curl http://127.0.0.1:11434/api/tags`

**Check 2:** Is ChatController recording tokens?
```bash
# Look in backend logs for:
# [ChatController] Could not record tokens: ...
```
- If error, check that TelemetryService bean is available

**Check 3:** Is SavingsController returning data?
```bash
# Browser DevTools → Network → Filter by "savings"
# Check response JSON
```
- If still hardcoded (42 calls), rebuild didn't apply changes
- Verify JAR was rebuilt: `ls -lt target/*.jar`

### Issue: Tokens = 0 or null

1. Check Ollama output format:
   ```bash
   curl -X POST http://127.0.0.1:11434/api/generate \
     -H "Content-Type: application/json" \
     -d '{"model":"mistral","prompt":"hello"}'
   ```
   Look for `prompt_eval_count` and `eval_count` in response

2. If missing, Ollama may need update:
   ```bash
   ollama pull mistral
   ```

## Expected Behavior After Fix

- ✓ First message generates tokens
- ✓ Dashboard Cost Comparison shows real data
- ✓ Token counts increase with each message
- ✓ Local cost updates dynamically
- ✓ Cloud provider costs calculated in real time
- ✓ `/v1/savings` endpoint returns live data (not hardcoded)
