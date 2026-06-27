@echo off
REM Quick cleanup script to kill stale Java processes and free port 8000

echo.
echo === OpenTron Java Process Cleanup ===
echo.

echo Finding Java processes...
tasklist /FI "IMAGENAME eq java.exe" | findstr java.exe
if %ERRORLEVEL% NEQ 0 (
    echo No Java processes found
    goto :eof
)

echo.
echo WARNING: This will kill ALL java.exe processes
echo Press Ctrl+C to cancel, or press any key to continue...
pause

echo.
echo Killing java.exe processes...
taskkill /IM java.exe /F

echo.
echo Waiting 2 seconds for port to be released...
timeout /t 2 /nobreak

echo.
echo Checking port 8000...
netstat -ano | findstr :8000
if %ERRORLEVEL% EQ 0 (
    echo Port 8000 still in use!
) else (
    echo Port 8000 is now free
)

echo.
echo Cleanup complete. You can now launch the app.
echo.
