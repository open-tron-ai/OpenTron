# Create GitHub Release Script (PowerShell)
# Usage: .\scripts\create-release.ps1 -Version v1.0.0

param(
    [string]$Version = "v1.0.0"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  GitHub Release Creator" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Validate version format
if ($Version -notmatch '^v\d+\.\d+\.\d+$') {
    Write-Host "❌ Invalid version format. Use: v1.0.0" -ForegroundColor Red
    exit 1
}

Write-Host "📦 Creating release: $Version" -ForegroundColor Green
Write-Host ""

# Check if tag exists
try {
    $tagExists = & git rev-parse $Version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Tag $Version already exists" -ForegroundColor Green
    }
}
catch {
    Write-Host "Creating tag $Version..." -ForegroundColor Yellow
    & git tag -a $Version -m "Release $Version"
    Write-Host "✓ Tag created" -ForegroundColor Green
}

# Push tag to GitHub
Write-Host "Pushing tag to GitHub..." -ForegroundColor Yellow
& git push origin $Version

Write-Host ""
Write-Host "✅ Release triggered!" -ForegroundColor Green
Write-Host ""
Write-Host "GitHub Actions will:" -ForegroundColor Cyan
Write-Host "  1. Create GitHub Release" -ForegroundColor Gray
Write-Host "  2. Build 5 native backends" -ForegroundColor Gray
Write-Host "     ├── Linux x86_64" -ForegroundColor Gray
Write-Host "     ├── Linux ARM64" -ForegroundColor Gray
Write-Host "     ├── macOS x86_64" -ForegroundColor Gray
Write-Host "     ├── macOS ARM64" -ForegroundColor Gray
Write-Host "     └── Windows x86_64" -ForegroundColor Gray
Write-Host "  3. Build 5 desktop apps" -ForegroundColor Gray
Write-Host "     ├── macOS DMG (Intel & Apple Silicon)" -ForegroundColor Gray
Write-Host "     ├── Linux AppImage (x86_64)" -ForegroundColor Gray
Write-Host "     ├── Linux DEB (ARM64)" -ForegroundColor Gray
Write-Host "     └── Windows EXE/MSI" -ForegroundColor Gray
Write-Host "  4. Upload all assets to release" -ForegroundColor Gray
Write-Host ""

# Get repo info
$repo = & git remote get-url origin
$repoName = $repo -replace '.*/(.*?)(?:\.git)?$', '$1'

Write-Host "📍 Monitor at:" -ForegroundColor Yellow
Write-Host "   https://github.com/$repoName/actions" -ForegroundColor White
Write-Host ""
Write-Host "📍 Release at:" -ForegroundColor Yellow
Write-Host "   https://github.com/$repoName/releases/tag/$Version" -ForegroundColor White
Write-Host ""

Write-Host "⏱️  Expected timeline:" -ForegroundColor Cyan
Write-Host "   • Native builds: ~30 min (parallel)" -ForegroundColor Gray
Write-Host "   • Desktop builds: ~20 min (parallel)" -ForegroundColor Gray
Write-Host "   • Total: ~40-50 minutes" -ForegroundColor Gray
Write-Host ""

Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan
