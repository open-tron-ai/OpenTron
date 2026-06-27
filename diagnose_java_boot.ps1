# Diagnostic script for Java backend boot failure

$ErrorActionPreference = "Stop"

Write-Host "=== OpenTron Java Backend Boot Diagnostics ===" -ForegroundColor Cyan
Write-Host ""

# 1. Check for Java
Write-Host "[1] Checking for Java binary..." -ForegroundColor Yellow
$javaFound = $false
$javaExe = ""

try {
    $javaExe = (Get-Command java.exe -ErrorAction Stop).Source
    Write-Host "  Found via PATH: $javaExe" -ForegroundColor Green
    $javaFound = $true
} catch {
    # Try JAVA_HOME
    if (Test-Path env:JAVA_HOME) {
        $javaExe = "$env:JAVA_HOME\bin\java.exe"
        if (Test-Path $javaExe) {
            Write-Host "  Found via JAVA_HOME: $javaExe" -ForegroundColor Green
            $javaFound = $true
        }
    }
}

if (-not $javaFound) {
    Write-Host "  Java not found!" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. Check for Java backend root
Write-Host "[2] Checking for Java backend root..." -ForegroundColor Yellow

$javaRoot = ""

if (Test-Path env:TRON_JAVA_ROOT) {
    $root = $env:TRON_JAVA_ROOT
    Write-Host "  Checking TRON_JAVA_ROOT: $root"
    if (Test-Path "$root\pom.xml") {
        Write-Host "  Found!" -ForegroundColor Green
        $javaRoot = $root
    }
}

if (-not $javaRoot -and (Test-Path env:OPENTRON_JAVA_ROOT)) {
    $root = $env:OPENTRON_JAVA_ROOT
    Write-Host "  Checking OPENTRON_JAVA_ROOT: $root"
    if (Test-Path "$root\pom.xml") {
        Write-Host "  Found!" -ForegroundColor Green
        $javaRoot = $root
    }
}

if (-not $javaRoot) {
    $homePath = $env:USERPROFILE
    $p = "$homePath\Documents\OpenTron\java\opentron-java"
    if (Test-Path "$p\pom.xml") {
        Write-Host "  Found at: $p" -ForegroundColor Green
        $javaRoot = $p
    }
}

if (-not $javaRoot) {
    Write-Host "  Java backend root not found!" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 3. Look for the CLI JAR
Write-Host "[3] Checking for CLI JAR..." -ForegroundColor Yellow

$jarPath = ""
$jar = "$javaRoot\cli\target\tron-cli-jar-with-dependencies.jar"
if (Test-Path $jar) {
    Write-Host "  Found: $jar" -ForegroundColor Green
    $jarPath = $jar
}

if (-not $jarPath) {
    $classes = "$javaRoot\cli\target\classes"
    if (Test-Path "$classes\io\opentron\cli\Main.class") {
        Write-Host "  Found compiled classes: $classes" -ForegroundColor Green
        $jarPath = $classes
    }
}

if (-not $jarPath) {
    Write-Host "  CLI JAR or classes not found!" -ForegroundColor Red
    Write-Host "  Run: cd $javaRoot" -ForegroundColor Yellow
    Write-Host "       mvn clean package -DskipTests" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# 4. Test the command
Write-Host "[4] Testing Java command..." -ForegroundColor Yellow
Write-Host "  Launching: java -cp ""$jarPath"" io.opentron.cli.Main serve --port 8000" -ForegroundColor Cyan
Write-Host ""

$tmpDir = "C:\temp"
if (-not (Test-Path $tmpDir)) { mkdir $tmpDir -Force | Out-Null }

$proc = Start-Process $javaExe -ArgumentList @('-cp', $jarPath, 'io.opentron.cli.Main', 'serve', '--port', '8000') `
    -NoNewWindow -PassThru -RedirectStandardError "$tmpDir\tron_err.txt" -RedirectStandardOutput "$tmpDir\tron_out.txt"

Write-Host "  Process started. PID: $($proc.Id)" -ForegroundColor Green
Write-Host "  Waiting 5 seconds..." -ForegroundColor Yellow

Start-Sleep -Seconds 5

if ($proc.HasExited) {
    Write-Host "  FAIL: Process exited! Exit code: $($proc.ExitCode)" -ForegroundColor Red
    Write-Host ""
    Write-Host "  --- STDERR ---" -ForegroundColor Yellow
    Get-Content "$tmpDir\tron_err.txt" -ErrorAction SilentlyContinue | Write-Host
    Write-Host ""
    Write-Host "  --- STDOUT ---" -ForegroundColor Yellow
    Get-Content "$tmpDir\tron_out.txt" -ErrorAction SilentlyContinue | Write-Host
    exit 1
} else {
    Write-Host "  SUCCESS: Process running. Testing health endpoint..." -ForegroundColor Green
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:8000/actuator/health" -ErrorAction Stop
        Write-Host "  SUCCESS: Health check passed!" -ForegroundColor Green
        Write-Host "  Boot working!" -ForegroundColor Green
    } catch {
        Write-Host "  INFO: Health check failed (server might still be initializing)" -ForegroundColor Yellow
        Write-Host "        Error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Stop-Process $proc -Force -ErrorAction SilentlyContinue
    Write-Host ""
    Write-Host "  Process terminated for cleanup" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Diagnostics Complete ===" -ForegroundColor Cyan
