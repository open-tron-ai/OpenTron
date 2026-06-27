@echo off
setlocal enabledelayedexpansion

REM Add Rust and Node.js to PATH
set "PATH=%USERPROFILE%\.cargo\bin;%ProgramFiles%\nodejs;C:\Program Files\Git\cmd;%PATH%"

REM Log file
set "LOG_FILE=%TEMP%\tauri_build.log"

echo. > "%LOG_FILE%"
echo Build started at %date% %time% >> "%LOG_FILE%"

REM Check tools
echo Checking tools... >> "%LOG_FILE%"
cargo --version >> "%LOG_FILE%" 2>&1
npm --version >> "%LOG_FILE%" 2>&1
node --version >> "%LOG_FILE%" 2>&1

REM Navigate and build
cd /d "C:\Users\ermis\Documents\OpenTron\frontend"
echo Building Tauri app... >> "%LOG_FILE%"
npm run tauri build >> "%LOG_FILE%" 2>&1

echo Build completed at %date% %time% >> "%LOG_FILE%"
echo Done! Output saved to %LOG_FILE%
type "%LOG_FILE%"
