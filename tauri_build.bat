@echo off
cd /d "C:\Users\ermis\Documents\OpenTron\frontend"
set "PATH=%USERPROFILE%\.cargo\bin;%ProgramFiles%\nodejs;%PATH%"
npm run tauri build
