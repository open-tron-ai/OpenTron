@echo off
REM OpenTron Full Stack Integration - Start Script (Windows)
REM Starts PostgreSQL, Backend, and Frontend

setlocal enabledelayedexpansion

echo.
echo ====================================================
echo   OpenTron - PostgreSQL Integration Stack
echo   Backend + Frontend + Database
echo ====================================================
echo.

REM Set environment variables
set JAVA_HOME=C:\Users\ciorica\Documents\jdk-21.0.11
set PATH=%JAVA_HOME%\bin;%PATH%
set MAVEN_HOME=C:\Users\ciorica\Documents\apache-maven-3.9.16
set PATH=%MAVEN_HOME%\bin;%PATH%
set NODE_HOME=C:\Users\ciorica\Documents\node-v24.18.0-win-x64
set PATH=%NODE_HOME%;%PATH%

set POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
set POSTGRES_USER=opentron
set POSTGRES_PASSWORD=opentron_secure_password
set ENGINE_HOST=http://localhost:11434

REM Step 1: Check Docker
echo [1/5] Checking Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: Docker is not installed or not in PATH
    echo.
    echo Please install Docker from: https://www.docker.com/products/docker-desktop
    echo.
    pause
    exit /b 1
)
echo OK: Docker found
echo.

REM Step 2: Start PostgreSQL
echo [2/5] Starting PostgreSQL...
docker ps | findstr "opentron-postgres" >nul
if errorlevel 1 (
    echo Starting PostgreSQL container...
    docker run -d --restart always --name opentron-postgres ^
        -e POSTGRES_DB=opentron ^
        -e POSTGRES_USER=opentron ^
        -e POSTGRES_PASSWORD=opentron_secure_password ^
        -p 5432:5432 ^
        -v postgres_data:/var/lib/postgresql/data ^
        postgres:16-alpine
    
    echo Waiting for PostgreSQL to start...
    timeout /t 5 /nobreak >nul
    echo OK: PostgreSQL started on port 5432
) else (
    echo OK: PostgreSQL already running
)
echo.

REM Step 3: Build Backend
echo [3/5] Building Backend...
cd /d java\opentron-java\backend

echo Building with Maven...
call %MAVEN_HOME%\bin\mvn.cmd clean package -DskipTests -q

if errorlevel 1 (
    echo.
    echo ERROR: Backend build failed
    echo.
    echo Try running manually:
    echo   cd java\opentron-java\backend
    echo   mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)

echo OK: Backend built successfully
echo.

REM Step 4: Start Backend in new window
echo [4/5] Starting Backend (in new window)...
cd /d "%~dp0java\opentron-java\backend"

start "OpenTron Backend" cmd /k ^
    "set JAVA_HOME=C:\Users\ciorica\Documents\jdk-21.0.11 ^& ^
    set PATH=%JAVA_HOME%\bin;%PATH% ^& ^
    set POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron ^& ^
    set POSTGRES_USER=opentron ^& ^
    set POSTGRES_PASSWORD=opentron_secure_password ^& ^
    C:\Users\ciorica\Documents\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run"

echo OK: Backend starting in separate window
echo Waiting 15 seconds for backend to start...
timeout /t 15 /nobreak >nul
echo.

REM Step 5: Start Frontend in new window
echo [5/5] Starting Frontend (in new window)...
cd /d "%~dp0frontend"

if not exist "node_modules" (
    echo Installing frontend dependencies (this may take a minute)...
    call %NODE_HOME%\npm.cmd install -q
)

start "OpenTron Frontend" cmd /k ^
    "set PATH=%NODE_HOME%;%PATH% ^& ^
    set PATH=C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\bin\Hostx64\x64;%PATH% ^& ^
    set LIB=C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\lib\x64;C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\um\x64;C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\ucrt\x64; ^& ^
    set INCLUDE=C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\include;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\um;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\shared;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\ucrt; ^& ^
    %NODE_HOME%\npm.cmd run tauri dev"

echo OK: Frontend starting in separate window
echo.
echo ====================================================
echo   SUCCESS: OpenTron Stack Started!
echo ====================================================
echo.
echo   Services:
echo   - PostgreSQL:  http://localhost:5432
echo   - Backend:     http://localhost:8000
echo   - Frontend:    Tauri window (auto-opens)
echo.
echo   Database:
echo   - Name:        opentron
echo   - User:        opentron
echo   - Password:    opentron_secure_password
echo.
echo   Next Steps:
echo   1. Wait for Frontend window to open (1-2 min)
echo   2. Navigate to Storage Dashboard
echo   3. Execute an agent to test
echo   4. Watch data appear in real-time
echo.
echo   Monitor:
echo   - Backend logs: in "OpenTron Backend" window
echo   - Frontend logs: in "OpenTron Frontend" window
echo.
echo ====================================================
echo.
echo Press any key to close this window
echo.
pause
