@echo off
REM Build script for OpenTron backend
REM This rebuilds the Java classes and restarts the app

setlocal enabledelayedexpansion

cd /d "%~dp0..\..\backend" || exit /b 1

echo [INFO] Building OpenTron backend...
echo [INFO] Current directory: %cd%

REM Check if Maven is installed
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found in PATH
    echo [INFO] Please install Maven or add it to your PATH
    echo [INFO] Download from: https://maven.apache.org/download.cgi
    exit /b 1
)

echo [INFO] Running: mvn clean compile -DskipTests=true
mvn clean compile -DskipTests=true

if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)

echo [SUCCESS] Build complete!
echo [INFO] Now restart your OpenTron backend service to load the new code
echo [INFO] Kill the backend process and restart it
pause
