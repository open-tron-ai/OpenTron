# Quick Reference: Where to Find Windows Installers

## TL;DR - Installer Locations

### **After Local Build** (via `.\scripts\build-native-desktop.ps1`)
```
frontend/src-tauri/target/x86_64-pc-windows-msvc/release/bundle/

├── msi/
│   └── OpenTron_1.0.0_x64.msi  ← Windows standard installer
│
└── nsis/
    └── OpenTron_1.0.0_x64_en-US.exe  ← Lightweight installer
```

### **After GitHub Release** (after pushing code + creating tag)
Visit: `https://github.com/your-org/OpenTron/releases/v1.0.0`

Download either:
- `OpenTron_1.0.0_x64.msi`
- `OpenTron_1.0.0_x64_en-US.exe`

---

## One-Line Install Commands

**From Local Build (MSI):**
```powershell
Start-Process "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\msi\OpenTron_1.0.0_x64.msi"
```

**From Local Build (NSIS):**
```powershell
Start-Process "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\nsis\OpenTron_1.0.0_x64_en-US.exe"
```

**Silent Install (MSI):**
```powershell
msiexec /i "OpenTron_1.0.0_x64.msi" /quiet /norestart
```

**Silent Install (NSIS):**
```powershell
.\OpenTron_1.0.0_x64_en-US.exe /S /D=C:\Program Files\OpenTron
```

---

## File Sizes

| Type | Size | Best For |
|------|------|----------|
| MSI | 120-150 MB | Enterprise/Group Policy |
| NSIS .exe | 100-130 MB | Consumer/quick install |

---

## Verification After Install

```powershell
# Check installed successfully
Get-Item "C:\Program Files\OpenTron\OpenTron.exe"

# Verify native backend is running (not Java)
Get-Process opentron-backend*

# Check startup performance (<2 sec = native working)
Measure-Command { & "C:\Program Files\OpenTron\OpenTron.exe" }
```

---

## Uninstall

**MSI:**
```powershell
msiexec /x "OpenTron_1.0.0_x64.msi" /quiet
```

**NSIS:**
```powershell
"C:\Program Files\OpenTron\Uninstall.exe" /S
```

**Via Control Panel:**
Settings → Apps → Installed apps → OpenTron → Uninstall

---

## If Installer Not Found

```powershell
# Check if build exists
Test-Path "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\msi\"

# If False, rebuild:
.\scripts\build-native-desktop.ps1

# Takes 45-70 minutes first time
# Re-runs are faster (cached)
```

---

**See WINDOWS_INSTALLER_LOCATIONS.md for full details and troubleshooting.**

