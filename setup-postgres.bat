@echo off
REM Setup PostgreSQL Docker container for OpenTron (Windows)
REM Run this script to start PostgreSQL and configure the environment

setlocal enabledelayedexpansion

cls
echo ==========================================
echo OpenTron PostgreSQL Docker Setup (Windows)
echo ==========================================
echo.

REM Check if Docker is running
echo [1/5] Checking Docker daemon...
docker ps > nul 2>&1
if errorlevel 1 (
    echo X Docker daemon is not running!
    echo    Please start Docker Desktop and try again.
    exit /b 1
)
echo [OK] Docker daemon is running
echo.

REM Check if container already exists
echo [2/5] Checking for existing PostgreSQL container...
docker ps -a --format "{{.Names}}" | findstr /x "opentron-postgres" > nul
if errorlevel 1 (
    echo    Creating new PostgreSQL container...
    docker run -d ^
        --name opentron-postgres ^
        -e POSTGRES_DB=opentron ^
        -e POSTGRES_USER=opentron ^
        -e POSTGRES_PASSWORD=opentron_secure_password ^
        -p 5432:5432 ^
        -v postgres_data:/var/lib/postgresql/data ^
        postgres:16-alpine
    if errorlevel 1 (
        echo [FAIL] Failed to create container
        exit /b 1
    )
    echo [OK] PostgreSQL container created
) else (
    docker ps --format "{{.Names}}" | findstr /x "opentron-postgres" > nul
    if errorlevel 1 (
        echo    Starting existing container...
        docker start opentron-postgres > nul
        if errorlevel 1 (
            echo [FAIL] Failed to start container
            exit /b 1
        )
    )
    echo [OK] Container is running
)
echo.

REM Wait for PostgreSQL to be ready
echo [3/5] Waiting for PostgreSQL to be ready...
setlocal enabledelayedexpansion
for /L %%i in (1,1,30) do (
    docker exec opentron-postgres pg_isready -U opentron > nul 2>&1
    if errorlevel 0 (
        echo [OK] PostgreSQL is ready
        goto postgres_ready
    )
    if %%i lss 30 (
        echo    Waiting... (%%i/30)
        timeout /t 1 /nobreak > nul
    )
)
echo [FAIL] PostgreSQL failed to start within 30 seconds
exit /b 1

:postgres_ready
echo.

REM Verify connection
echo [4/5] Verifying connection...
docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT version();" > nul 2>&1
if errorlevel 1 (
    echo [FAIL] Connection failed
    exit /b 1
)
echo [OK] Connection successful
echo.

REM Display connection information
echo [5/5] Configuration Summary
echo ==========================================
echo [SUCCESS] PostgreSQL is ready!
echo.
echo Connection Details:
echo   Host:     localhost
echo   Port:     5432
echo   Database: opentron
echo   User:     opentron
echo   Password: opentron_secure_password
echo.
echo Docker Container:
echo   Name:     opentron-postgres
echo   Image:    postgres:16-alpine
echo   Status:   Running
echo.
echo Environment Variables (set these for your Java backend):
echo   POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
echo   POSTGRES_USER=opentron
echo   POSTGRES_PASSWORD=opentron_secure_password
echo.
echo Useful Docker Commands:
echo   docker logs opentron-postgres          # View logs
echo   docker ps                              # List running containers
echo   docker stop opentron-postgres          # Stop container
echo   docker start opentron-postgres         # Start container
echo   docker rm opentron-postgres            # Remove container
echo.
echo Next Steps:
echo   1. Set the environment variables above in PowerShell or CMD
echo   2. cd java\opentron-java\backend
echo   3. mvn clean package -DskipTests
echo   4. mvn spring-boot:run
echo.
echo ==========================================
pause
