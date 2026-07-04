# OpenTron Full Stack Integration - Start Script (PowerShell)
# Starts PostgreSQL, Backend, and Frontend

Write-Host ""
Write-Host "===================================================="
Write-Host "  OpenTron - PostgreSQL Integration Stack"
Write-Host "  Backend + Frontend + Database"
Write-Host "===================================================="
Write-Host ""

# Set environment variables
$env:JAVA_HOME = "C:\Users\ciorica\Documents\jdk-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:MAVEN_HOME = "C:\Users\ciorica\Documents\apache-maven-3.9.16"
$env:PATH = "$env:MAVEN_HOME\bin;$env:PATH"
$env:NODE_HOME = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64"
$env:PATH = "$env:NODE_HOME;$env:PATH"

$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER = "opentron"
$env:POSTGRES_PASSWORD = "opentron_secure_password"
$env:ENGINE_HOST = "http://localhost:11434"

# Step 1: Check Docker
Write-Host "[1/5] Checking Docker..."
try {
    docker --version | Out-Null
    Write-Host "OK: Docker found"
} catch {
    Write-Host "ERROR: Docker is not installed or not in PATH"
    Write-Host "Please install Docker from: https://www.docker.com/products/docker-desktop"
    exit 1
}
Write-Host ""

# Step 2: Start PostgreSQL
Write-Host "[2/5] Starting PostgreSQL..."
$postgresRunning = docker ps | Select-String "opentron-postgres"
if ($postgresRunning) {
    Write-Host "OK: PostgreSQL already running"
} else {
    Write-Host "Starting PostgreSQL container..."
    docker run -d --restart always --name opentron-postgres `
        -e POSTGRES_DB=opentron `
        -e POSTGRES_USER=opentron `
        -e POSTGRES_PASSWORD=opentron_secure_password `
        -p 5432:5432 `
        -v postgres_data:/var/lib/postgresql/data `
        postgres:16-alpine | Out-Null
    
    Write-Host "Waiting for PostgreSQL to start..."
    Start-Sleep -Seconds 5
    Write-Host "OK: PostgreSQL started on port 5432"
}
Write-Host ""

# Step 3: Build Backend
Write-Host "[3/5] Building Backend..."
Set-Location "java\opentron-java\backend"
Write-Host "Building with Maven..."
& $env:MAVEN_HOME\bin\mvn.cmd clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Backend build failed"
    exit 1
}
Write-Host "OK: Backend built successfully"
Write-Host ""

# Step 4: Start Backend
Write-Host "[4/5] Starting Backend (in new window)..."
$backendScript = {
    $env:JAVA_HOME = "C:\Users\ciorica\Documents\jdk-21.0.11"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $env:MAVEN_HOME = "C:\Users\ciorica\Documents\apache-maven-3.9.16"
    $env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/opentron"
    $env:POSTGRES_USER = "opentron"
    $env:POSTGRES_PASSWORD = "opentron_secure_password"
    
    Set-Location "C:\Users\ciorica\Documents\OpenTron\java\opentron-java\backend"
    Write-Host ""
    Write-Host "Starting Backend Server..."
    Write-Host ""
    & $env:MAVEN_HOME\bin\mvn.cmd spring-boot:run
}

Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendScript -WindowStyle Normal -PassThru | Out-Null
Write-Host "OK: Backend starting in separate window"
Write-Host "Waiting 20 seconds for backend to start..."
Start-Sleep -Seconds 20
Write-Host ""

# Step 5: Start Frontend
Write-Host "[5/5] Starting Frontend (in new window)..."
$frontendScript = {
    $env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;$env:PATH"
    $env:PATH = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\bin\Hostx64\x64;$env:PATH"
    $env:LIB = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\lib\x64;C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\um\x64;C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\ucrt\x64;"
    $env:INCLUDE = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\include;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\um;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\shared;C:\Users\ciorica\Documents\10\Include\10.0.26100.0\ucrt"
    
    Set-Location "C:\Users\ciorica\Documents\OpenTron\frontend"
    Write-Host ""
    Write-Host "Starting Frontend Server..."
    Write-Host ""
    
    if (-not (Test-Path "node_modules")) {
        Write-Host "Installing frontend dependencies..."
        & npm install -q
    }
    
    & npm run tauri dev
}

Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendScript -WindowStyle Normal -PassThru | Out-Null
Write-Host "OK: Frontend starting in separate window"
Write-Host ""

Write-Host "===================================================="
Write-Host "  SUCCESS: OpenTron Stack Started!"
Write-Host "===================================================="
Write-Host ""
Write-Host "  Services:"
Write-Host "  - PostgreSQL:  http://localhost:5432"
Write-Host "  - Backend:     http://localhost:8000"
Write-Host "  - Frontend:    Tauri window (auto-opens)"
Write-Host ""
Write-Host "  Database:"
Write-Host "  - Name:        opentron"
Write-Host "  - User:        opentron"
Write-Host "  - Password:    opentron_secure_password"
Write-Host ""
Write-Host "  Next Steps:"
Write-Host "  1. Wait for Frontend window to open (1-2 min)"
Write-Host "  2. Navigate to Storage Dashboard"
Write-Host "  3. Execute an agent to test"
Write-Host "  4. Watch data appear in real-time"
Write-Host ""
Write-Host "  Monitor:"
Write-Host "  - Backend logs: in PowerShell window #1"
Write-Host "  - Frontend logs: in PowerShell window #2"
Write-Host ""
Write-Host "===================================================="
Write-Host ""

# Keep this window open
Write-Host "This window will close when you press Ctrl+C"
Write-Host ""
while ($true) { Start-Sleep -Seconds 1 }
