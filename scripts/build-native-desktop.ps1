#!/usr/bin/env pwsh
# Local build script: Compile native backend and bundle into desktop app (PowerShell)
# Usage: .\scripts\build-native-desktop.ps1

param(
    [string]$Platform = "auto",
    [string]$Arch = "auto"
)

$ErrorActionPreference = "Stop"

# Colors
function Write-Success { Write-Host $args -ForegroundColor Green }
function Write-Error-Custom { Write-Host $args -ForegroundColor Red }
function Write-Info { Write-Host $args -ForegroundColor Cyan }
function Write-Warn { Write-Host $args -ForegroundColor Yellow }

Write-Info "=== OpenTron Native Desktop Builder (PowerShell) ==="
Write-Host ""

# Detect platform if not specified
if ($Platform -eq "auto") {
    $Platform = if ($PSVersionTable.Platform -eq "Unix") {
        if ((uname -s) -match "Darwin") { "macos" } else { "linux" }
    } else {
        "windows"
    }
}

if ($Arch -eq "auto") {
    $Arch = if ($PSVersionTable.Platform -eq "Unix") {
        $uname_m = uname -m
        if ($uname_m -match "x86_64|amd64") { "x86_64" } 
        elseif ($uname_m -match "aarch64|arm64") { "aarch64" }
        else { $uname_m }
    } else {
        if ([Environment]::Is64BitProcess) { "x86_64" } else { "x86" }
    }
}

Write-Host "Platform: $(Write-Host -NoNewline -ForegroundColor Yellow $Platform) ($(Write-Host -NoNewline -ForegroundColor Yellow $Arch))"
Write-Host ""

# Step 1: Check prerequisites
Write-Info "[1/5] Checking prerequisites..."
Write-Host ""

$prereqs = @{
    "java" = "java -version"
    "mvn" = "mvn -version"
    "node" = "node --version"
    "npm" = "npm --version"
    "cargo" = "cargo --version"
}

foreach ($cmd in $prereqs.Keys) {
    try {
        $output = & $cmd 2>&1
        Write-Success "✓ $cmd"
    } catch {
        Write-Error-Custom "✗ $cmd not found"
        exit 1
    }
}

Write-Host ""

# Step 2: Build native backend
Write-Info "[2/5] Building native backend for $Platform-$Arch..."
Write-Host ""

Push-Location "java/opentron-java/backend"

# Determine profile
$nativeProfile = switch -Regex ("$Platform-$Arch") {
    "^linux-x86_64$" { "native-linux-x86_64" }
    "^linux-aarch64$" { "native-linux-aarch64" }
    "^macos-x86_64$" { "native-macos-x86_64" }
    "^macos-aarch64$" { "native-macos-aarch64" }
    "^windows-x86_64$" { "native-windows-x86_64" }
    default { "" }
}

if ($nativeProfile) {
    Write-Host "Profile: $(Write-Host -NoNewline -ForegroundColor Yellow $nativeProfile)"
    Write-Host ""
    & mvn -DskipTests=true -P$nativeProfile clean package
} else {
    Write-Warn "No specific profile for $Platform-$Arch, using auto-detection"
    & mvn -DskipTests=true clean package
}

if ($LASTEXITCODE -ne 0) {
    Write-Error-Custom "Native build failed"
    exit 1
}

# Find built binary
$nativeBinary = Get-ChildItem -Path "target" -Filter "opentron-backend*" -File | Select-Object -First 1

if (-not $nativeBinary) {
    Write-Error-Custom "✗ Native binary not found in target/"
    exit 1
}

$binarySize = "{0:F1} MB" -f ($nativeBinary.Length / 1MB)
Write-Success "✓ Native binary built: $binarySize"
Write-Host "  Location: $($nativeBinary.FullName)"
Write-Host ""

Pop-Location

# Step 3: Prepare frontend sidecar
Write-Info "[3/5] Setting up frontend sidecar..."
Write-Host ""

$sidecarPath = "frontend/src-tauri/sidecar"
if (-not (Test-Path $sidecarPath)) {
    New-Item -ItemType Directory -Path $sidecarPath -Force | Out-Null
}

Copy-Item $nativeBinary.FullName -Destination $sidecarPath
Write-Success "✓ Native binary copied to sidecar"
Write-Host "  Sidecar: $sidecarPath"
Write-Host ""

# Step 4: Build frontend
Write-Info "[4/5] Building frontend..."
Write-Host ""

Push-Location "frontend"

if (-not (Test-Path "node_modules")) {
    Write-Host "Installing dependencies..."
    & npm ci
}

& npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Error-Custom "Frontend build failed"
    exit 1
}

Write-Success "✓ Frontend built"
Write-Host ""

Pop-Location

# Step 5: Build desktop app
Write-Info "[5/5] Building Tauri desktop app..."
Write-Host ""

Push-Location "frontend/src-tauri"

# Determine build target
$target = switch -Regex ("$Platform-$Arch") {
    "^macos-x86_64$" { "x86_64-apple-darwin" }
    "^macos-aarch64$" { "aarch64-apple-darwin" }
    "^linux-x86_64$" { "x86_64-unknown-linux-gnu" }
    "^linux-aarch64$" { "aarch64-unknown-linux-gnu" }
    "^windows-x86_64$" { "x86_64-pc-windows-msvc" }
    default { "x86_64-pc-windows-msvc" }
}

Write-Host "Build target: $(Write-Host -NoNewline -ForegroundColor Yellow $target)"
Write-Host ""

# Ensure target is installed
& rustup target add $target

# Build release
Write-Host "Building Tauri app..."
& cargo build --release --target $target

if ($LASTEXITCODE -ne 0) {
    Write-Error-Custom "Desktop build failed"
    exit 1
}

Write-Success "✓ Desktop app built"
Write-Host ""

Pop-Location

# Step 6: Locate and display output
Write-Info "[✓] Build complete!"
Write-Host ""

$bundlePath = "frontend/src-tauri/target/$target/release/bundle"

if (Test-Path $bundlePath) {
    Write-Success "Output locations:"
    Write-Host ""
    
    $bundleTypes = @("msi", "nsis", "dmg", "appimage", "deb")
    foreach ($bundleType in $bundleTypes) {
        $typePath = Join-Path $bundlePath $bundleType
        if (Test-Path $typePath) {
            Get-ChildItem $typePath | ForEach-Object {
                Write-Host "  $(Write-Host -NoNewline -ForegroundColor Yellow $_.FullName)"
            }
        }
    }
} else {
    Write-Warn "Bundle path not found: $bundlePath"
}

Write-Host ""
Write-Success "Next: Test the app and verify native backend is bundled"
