@echo off
REM Test coordinator endpoints (Windows version)
REM Requires curl and jq (or just curl)

setlocal enabledelayedexpansion

set BASE_URL=http://127.0.0.1:8000

echo.
echo 🧪 Testing OpenTron Coordinator Endpoints
echo ==========================================
echo.

REM Test 1: Agent Statuses
echo 📊 Test 1: Get Agent Statuses
echo URL: %BASE_URL%/v1/agents/status
echo.

curl -s -X GET "%BASE_URL%/v1/agents/status" ^
  -H "Content-Type: application/json"

echo.
echo ---
echo.

REM Test 2: Coordinator Request  
echo 🤖 Test 2: Process Coordinator Request
echo URL: %BASE_URL%/v1/agents/coordinate
echo Request: optimize cache layer
echo.

curl -s -X POST "%BASE_URL%/v1/agents/coordinate" ^
  -H "Content-Type: application/json" ^
  -d "{\"request\":\"optimize the cache layer for high throughput\",\"context\":\"We have 2M daily requests\"}"

echo.
echo ---
echo.

REM Test 3: Send Task to Backend Agent
echo 📤 Test 3: Send Task to Backend Agent
echo URL: %BASE_URL%/v1/agents/task
echo.

curl -s -X POST "%BASE_URL%/v1/agents/task" ^
  -H "Content-Type: application/json" ^
  -d "{\"agent\":\"backend\",\"task\":\"review database indexes for performance\"}"

echo.
echo.
echo ==========================================
echo ✅ Tests complete!
echo.
echo 📝 Notes:
echo - All responses should have HTTP 200 status
echo - Coordinator should respond within 2-3 seconds
echo - If response is not JSON, the endpoint may be failing
echo - If you see error messages, check the backend logs
echo.
pause
