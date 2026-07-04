#!/bin/bash
# Test coordinator endpoints
# Run from the frontend terminal or anywhere

echo "🧪 Testing OpenTron Coordinator Endpoints"
echo "=========================================="
echo ""

BASE_URL="http://127.0.0.1:8000"

# Test 1: Agent Statuses
echo "📊 Test 1: Get Agent Statuses"
echo "URL: $BASE_URL/v1/agents/status"
echo ""
curl -s -X GET "$BASE_URL/v1/agents/status" \
  -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "❌ Failed or jq not installed"
echo ""
echo "---"
echo ""

# Test 2: Coordinator Request
echo "🤖 Test 2: Process Coordinator Request"
echo "URL: $BASE_URL/v1/agents/coordinate"
echo "Request: optimize cache layer"
echo ""
curl -s -X POST "$BASE_URL/v1/agents/coordinate" \
  -H "Content-Type: application/json" \
  -d '{"request":"optimize the cache layer for high throughput","context":"We have 2M daily requests"}' | jq '.' 2>/dev/null || echo "❌ Failed or jq not installed"
echo ""
echo "---"
echo ""

# Test 3: Send Task to Specific Agent
echo "📤 Test 3: Send Task to Backend Agent"
echo "URL: $BASE_URL/v1/agents/task"
echo ""
TASK_RESPONSE=$(curl -s -X POST "$BASE_URL/v1/agents/task" \
  -H "Content-Type: application/json" \
  -d '{"agent":"backend","task":"review database indexes for performance"}')

echo "$TASK_RESPONSE" | jq '.' 2>/dev/null || echo "❌ Failed or jq not installed"

# Extract taskId for polling
TASK_ID=$(echo "$TASK_RESPONSE" | jq -r '.task_id' 2>/dev/null)

if [ ! -z "$TASK_ID" ] && [ "$TASK_ID" != "null" ]; then
  echo ""
  echo "⏳ Test 4: Poll Task Result"
  echo "URL: $BASE_URL/v1/agents/task/$TASK_ID"
  echo ""
  sleep 1
  curl -s -X GET "$BASE_URL/v1/agents/task/$TASK_ID" \
    -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "❌ Failed or jq not installed"
fi

echo ""
echo "=========================================="
echo "✅ Tests complete!"
echo ""
echo "📝 Notes:"
echo "- All responses should have HTTP 200 status"
echo "- Coordinator should respond within 2-3 seconds"
echo "- Task polling should show 'pending' then 'completed'"
echo "- If 'is_mock' is true, LLM service is unavailable (fallback active)"
