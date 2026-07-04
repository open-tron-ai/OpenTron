@echo off
REM PostgreSQL Integration Setup Script for OpenTron
REM This script sets up all environment variables and starts the backend

echo.
echo ============================================================
echo  OpenTron PostgreSQL Integration Setup
echo ============================================================
echo.

REM Set Java Home
set JAVA_HOME=C:\Users\ciorica\Documents\jdk-21.0.11
set PATH=%JAVA_HOME%\bin;%PATH%

REM Set Maven Home
set MAVEN_HOME=C:\Users\ciorica\Documents\apache-maven-3.9.16
set PATH=%MAVEN_HOME%\bin;%PATH%

REM Set PostgreSQL Configuration
set POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
set POSTGRES_USER=opentron
set POSTGRES_PASSWORD=opentron_secure_password
set ENGINE_HOST=http://localhost:11434

echo [1/5] Java and Maven configuration set
echo.

REM Verify Docker is running
echo [2/5] Verifying Docker and PostgreSQL setup...
docker ps | findstr "opentron-postgres" > nul
if errorlevel 1 (
    echo.
    echo WARNING: PostgreSQL container not found!
    echo Run this command first to start PostgreSQL:
    echo.
    echo docker run -d --restart always --name opentron-postgres ^
    echo   -e POSTGRES_DB=opentron ^
    echo   -e POSTGRES_USER=opentron ^
    echo   -e POSTGRES_PASSWORD=opentron_secure_password ^
    echo   -p 5432:5432 ^
    echo   -v postgres_data:/var/lib/postgresql/data ^
    echo   postgres:16-alpine
    echo.
    echo Waiting 30 seconds before continuing...
    timeout /t 30 /nobreak
) else (
    echo PostgreSQL container is running
)

echo.

REM Navigate to backend
cd java\opentron-java\backend

echo [3/5] Building backend with Maven...
echo.
call %MAVEN_HOME%\bin\mvn.cmd clean package -DskipTests -q
if errorlevel 1 (
    echo.
    echo ERROR: Maven build failed!
    echo.
    pause
    exit /b 1
)

echo.
echo [4/5] Backend build completed successfully
echo.

REM Show startup information
echo ============================================================
echo [5/5] Starting OpenTron Backend
echo ============================================================
echo.
echo Configuration:
echo   Java Home: %JAVA_HOME%
echo   Maven Home: %MAVEN_HOME%
echo   PostgreSQL URL: %POSTGRES_URL%
echo   PostgreSQL User: %POSTGRES_USER%
echo   Ollama Engine: %ENGINE_HOST%
echo.
echo Starting backend...
echo.

REM Start the backend
call %MAVEN_HOME%\bin\mvn.cmd spring-boot:run

REM If backend exits, show message
echo.
echo Backend has stopped. Press any key to exit.
pause

exit /b 0
