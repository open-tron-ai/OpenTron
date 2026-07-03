@echo off
REM Build script for OpenTron - Run this from Command Prompt (cmd.exe)

cd /d "C:\Users\ciorica\Documents\OpenTron\java\opentron-java"

echo.
echo ========================================
echo Building OpenTron Multi-Agent System
echo ========================================
echo.

REM Try to find Maven in PATH first
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Found Maven in PATH
    mvn clean package -DskipTests
) else (
    REM Try common Maven location
    if exist "C:\Program Files\Apache Maven\apache-maven-3.9.0\bin\mvn.bat" (
        echo Using Maven from: C:\Program Files\Apache Maven\apache-maven-3.9.0
        "C:\Program Files\Apache Maven\apache-maven-3.9.0\bin\mvn.bat" clean package -DskipTests
    ) else (
        REM Try Maven wrapper
        if exist "mvnw.bat" (
            echo Using Maven wrapper
            mvnw.bat clean package -DskipTests
        ) else (
            echo Maven not found!
            echo Please install Maven or add it to your PATH
            exit /b 1
        )
    )
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESS!
    echo ========================================
    echo.
    echo Next steps:
    echo 1. Start backend:
    echo    java -jar backend\target\opentron-java-backend-1.0-SNAPSHOT.jar
    echo.
    echo 2. Test the agents:
    echo    curl -X POST http://localhost:8000/v1/agents/coordinate -H "Content-Type: application/json" -d "{\"request\":\"test\"}"
    echo.
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo.
    echo Check the errors above and fix them.
    exit /b 1
)
