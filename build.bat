@echo off
setlocal enabledelayedexpansion

REM Add Rust and Node.js to PATH
set "PATH=%USERPROFILE%\.cargo\bin;%ProgramFiles%\nodejs;%PATH%"

REM Verify tools are available
echo Checking tools...
cargo --version
node --version
npm --version

REM Navigate to frontend and build
cd /d "C:\Users\ermis\Documents\OpenTron\frontend"
echo Building Tauri app...
npm run tauri build

