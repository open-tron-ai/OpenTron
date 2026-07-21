# Windows Installer Output Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                     BUILD PROCESS                                   │
│                                                                     │
│  .\scripts\build-native-desktop.ps1                                │
│           │                                                         │
│           ├─→ [1] Build Native Backend (GraalVM)                   │
│           │       └─→ java/opentron-java/backend/target/           │
│           │           └─→ opentron-backend-windows-x86_64.exe (58MB)
│           │                                                         │
│           ├─→ [2] Copy Native to Sidecar                           │
│           │       └─→ frontend/src-tauri/sidecar/                  │
│           │                                                         │
│           ├─→ [3] Install Frontend Dependencies                    │
│           │       └─→ npm install (src-tauri/)                     │
│           │                                                         │
│           ├─→ [4] Build Desktop App (Cargo)                        │
│           │       └─→ frontend/src-tauri/target/release/           │
│           │                                                         │
│           └─→ [5] Create Installers ⭐                             │
│                   │                                                 │
│                   ├─→ MSI INSTALLER                                │
│                   │   frontend/src-tauri/target/                   │
│                   │   x86_64-pc-windows-msvc/release/              │
│                   │   bundle/msi/                                  │
│                   │   └─→ 📦 OpenTron_1.0.0_x64.msi (130 MB)       │
│                   │                                                 │
│                   └─→ NSIS INSTALLER                               │
│                       frontend/src-tauri/target/                   │
│                       x86_64-pc-windows-msvc/release/              │
│                       bundle/nsis/                                 │
│                       └─→ 📦 OpenTron_1.0.0_x64_en-US.exe (110 MB) │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                ┌─────────────────┴─────────────────┐
                │                                   │
                ▼                                   ▼
        ┌──────────────────┐           ┌──────────────────┐
        │  LOCAL INSTALL   │           │ CI/CD RELEASE    │
        │                  │           │                  │
        │ Double-click     │           │ Push to GitHub:  │
        │ .msi/.exe file   │           │ git push main    │
        │                  │           │ git tag v1.0.0   │
        │ Or:              │           │ git push --tags  │
        │ msiexec /i ...   │           │                  │
        │                  │           │ GitHub Actions   │
        └──────────────────┘           │ builds all 5     │
                │                      │ platforms:       │
                │                      │ • Linux x86_64   │
                │                      │ • Linux ARM64    │
                │                      │ • macOS x86_64   │
                │                      │ • macOS ARM64    │
                │                      │ • Windows x86_64 │
                │                      │                  │
                │                      │ Artifacts:       │
                │                      │ • .msi files     │
                │                      │ • .exe files     │
                │                      │ • .dmg files     │
                │                      │ • .deb/.AppImage │
                │                      │ • Native binaries│
                │                      │ • CHECKSUMS     │
                │                      │                  │
                │                      └────────┬─────────┘
                │                               │
                └───────────────────┬───────────┘
                                    │
                                    ▼
                        ┌─────────────────────┐
                        │  INSTALLED APP      │
                        │                     │
                        │ C:\Program Files\   │
                        │ OpenTron\           │
                        │ ├── OpenTron.exe    │
                        │ ├── opentron-backend│
                        │ │  -windows-x86_64  │
                        │ │  .exe (native)    │
                        │ └── other files     │
                        │                     │
                        │ Start Menu entry    │
                        │ ▶ Quick Launch      │
                        │ < 2 sec startup     │
                        │ (native backend)    │
                        └─────────────────────┘
```

---

## Path Reference

### Input (Before Build)
```
java/opentron-java/backend/
  └── pom.xml (has native-image profiles)

frontend/src-tauri/
  ├── src/
  ├── Cargo.toml
  └── tauri.conf.json
```

### Output After Build (Where to Find Installers)
```
frontend/src-tauri/target/
└── x86_64-pc-windows-msvc/
    └── release/
        └── bundle/
            ├── msi/
            │   └── ✅ OpenTron_1.0.0_x64.msi ← INSTALL THIS
            │
            ├── nsis/
            │   └── ✅ OpenTron_1.0.0_x64_en-US.exe ← OR THIS
            │
            └── exe/
                └── OpenTron.exe (updater)
```

### Installed Location (After Running Installer)
```
C:\Program Files\OpenTron\
├── OpenTron.exe
├── opentron-backend-windows-x86_64.exe (native binary - 58 MB)
├── resources/
├── LICENSE
└── Uninstall.exe
```

---

## File Size Breakdown

| Component | Size | Located |
|-----------|------|---------|
| Native backend binary | 58 MB | `java/.../target/opentron-backend-windows-x86_64.exe` |
| Frontend app binary | 20 MB | `frontend/.../release/` |
| Dependencies/resources | 30-40 MB | Bundled in installer |
| **Total MSI** | **120-150 MB** | `.../bundle/msi/*.msi` |
| **Total NSIS EXE** | **100-130 MB** | `.../bundle/nsis/*.exe` |

---

## Steps to Get Installer

### Quick Path
```powershell
# 1. Build
.\scripts\build-native-desktop.ps1

# 2. Find it
Get-ChildItem "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\msi\*"

# 3. Install
Start-Process "...\OpenTron_1.0.0_x64.msi"

# 4. Verify (should show in Start menu)
Start-Process explorer "shell:appsFolder"
```

### GitHub Release Path
```bash
# 1. Push code
git add .
git commit -m "release: ready for v1.0.0"
git push origin main

# 2. Create release tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# 3. Wait for CI/CD (~30-40 min)

# 4. Download from GitHub
# https://github.com/your-org/OpenTron/releases/v1.0.0
#   └── OpenTron_1.0.0_x64.msi ← Download here
```

---

## Verification Checklist

After opening installer, verify:

- [ ] Installer launches without error
- [ ] Installation location is `C:\Program Files\OpenTron\`
- [ ] Start menu shortcut created
- [ ] App launches (<2 sec)
- [ ] Setup screen shows: "Starting native API server..."
- [ ] Native backend process visible in Task Manager
- [ ] Not showing Java process

---

**See WINDOWS_INSTALLER_LOCATIONS.md for full troubleshooting guide.**

