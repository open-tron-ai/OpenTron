@echo off
REM OpenTron Backend Launcher for Windows
REM This script is run by the installer to set up the backend launcher

setlocal enabledelayedexpansion

set "HOME_DIR=%USERPROFILE%"
set "SIDECAR_DIR=!HOME_DIR!\.OpenTron\sidecar"
set "BACKEND_JAR=!SIDECAR_DIR!\backend.jar"
set "JRE_BIN=!SIDECAR_DIR!\jre\bin\java.exe"

REM Create .OpenTron directory if it doesn't exist
if not exist "!HOME_DIR!\.OpenTron" (
  mkdir "!HOME_DIR!\.OpenTron"
)

REM Copy files from installer sidecar to user home
if exist "sidecar\backend.jar" (
  copy "sidecar\backend.jar" "!SIDECAR_DIR!\"
)

if exist "sidecar\jre" (
  robocopy "sidecar\jre" "!SIDECAR_DIR!\jre" /E /Y >nul
)

REM Verify
if not exist "!BACKEND_JAR!" (
  echo ERROR: backend.jar not found
  exit /b 1
)

if not exist "!JRE_BIN!" (
  echo ERROR: Java runtime not found
  exit /b 1
)

REM Start backend in background
start "OpenTron Backend" "!JRE_BIN!" -Dspring.profiles.active=embedded -jar "!BACKEND_JAR!" --server.port=8000

REM Wait for backend to start
timeout /t 3 /nobreak

REM Start the Tauri app
start "" "%~dp0OpenTron.exe"
