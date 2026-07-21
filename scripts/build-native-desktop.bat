@echo off
REM Local build script: Compile native backend and bundle into desktop app (Windows)
REM Usage: build-native-desktop.bat

setlocal enabledelayedexpansion

echo.
echo === OpenTron Native Desktop Builder (Windows) ===
echo.

REM Check prerequisites
echo [1/5] Checking prerequisites...

for %%cmd in (java.exe mvn.cmd npm.cmd cargo.exe rustup.exe) do (
  where %%cmd >nul 2>nul
  if !errorlevel! neq 0 (
    echo X %%cmd not found
    exit /b 1
  ) else (
    echo + %%cmd
  )
)

REM Step 1: Build native backend
echo.
echo [2/5] Building native backend for Windows x86_64...

cd java\opentron-java\backend

mvn -DskipTests=true -Pnative-windows-x86_64 clean package
if !errorlevel! neq 0 (
  echo Error: Native build failed
  exit /b 1
)

REM Find built binary
for /f "delims=" %%f in ('dir /b target\opentron-backend*.exe 2^>nul') do (
  set "NATIVE_BINARY=%%f"
)

if "!NATIVE_BINARY!"=="" (
  echo Error: Native binary not found in target\
  exit /b 1
)

echo + Native binary: !NATIVE_BINARY!

cd ..\..\..

REM Step 2: Prepare frontend sidecar
echo.
echo [3/5] Setting up frontend sidecar...

if not exist "frontend\src-tauri\sidecar" mkdir frontend\src-tauri\sidecar
copy "java\opentron-java\backend\target\!NATIVE_BINARY!" "frontend\src-tauri\sidecar\"

echo + Native binary copied to sidecar

REM Step 3: Build frontend
echo.
echo [4/5] Building frontend...

cd frontend

if not exist "node_modules" (
  call npm ci
)

call npm run build
if !errorlevel! neq 0 (
  echo Error: Frontend build failed
  exit /b 1
)

echo + Frontend built

cd ..

REM Step 4: Build desktop app
echo.
echo [5/5] Building Tauri desktop app...

cd frontend\src-tauri

REM Ensure Rust target is installed
call rustup target add x86_64-pc-windows-msvc

REM Build release
call cargo build --release --target x86_64-pc-windows-msvc
if !errorlevel! neq 0 (
  echo Error: Desktop build failed
  exit /b 1
)

cd ..\..

echo.
echo === Build complete! ===
echo.

REM Display output locations
setlocal enabledelayedexpansion
for /f "delims=" %%d in ('dir /ad /b "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\*" 2^>nul') do (
  echo Output: frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\%%d
  for /f "delims=" %%f in ('dir /b "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\%%d\*" 2^>nul') do (
    echo   - %%f
  )
)

echo.
echo Next: Test the app and verify native backend is bundled
echo.
