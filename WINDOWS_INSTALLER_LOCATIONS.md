# Windows Installer Output Locations

## Local Build Output

When you build locally using `.\scripts\build-native-desktop.ps1`, the Windows installer will be at:

```
frontend/src-tauri/target/x86_64-pc-windows-msvc/release/bundle/
```

### Possible Installer Types

**Option 1: MSI Installer (Recommended for enterprises)**
```
frontend/src-tauri/target/x86_64-pc-windows-msvc/release/bundle/msi/
  └── OpenTron_<version>_x64.msi  (~100-150 MB)
```
- Standard Windows installer format
- Can be deployed via Group Policy
- Uninstall via Control Panel → Programs

**Option 2: NSIS Installer (Smaller, faster)**
```
frontend/src-tauri/target/x86_64-pc-windows-msvc/release/bundle/nsis/
  └── OpenTron_<version>_x64_en-US.exe  (~90-120 MB)
```
- Lightweight executable installer
- Quick installation
- Portable deployment

### Complete Directory Structure
```
frontend/src-tauri/target/x86_64-pc-windows-msvc/release/
├── bundle/
│   ├── msi/
│   │   └── OpenTron_1.0.0_x64.msi
│   ├── nsis/
│   │   └── OpenTron_1.0.0_x64_en-US.exe
│   └── exe/  (also available)
│       └── OpenTron.exe (portable/updater)
└── (other Rust artifacts)
```

---

## GitHub Actions CI/CD Output

After pushing to GitHub and triggering CI/CD, installers will be available at:

### GitHub Releases Page
Navigate to: **GitHub → Code → Releases → v1.0.0**

```
OpenTron v1.0.0 Release
├── Windows
│   ├── OpenTron_1.0.0_x64.msi          (~130 MB)
│   ├── OpenTron_1.0.0_x64_en-US.exe    (~110 MB)
│   └── opentron-backend-windows-x86_64.exe  (~58 MB - native binary)
├── macOS
│   ├── OpenTron_1.0.0_x64.dmg          (~140 MB - Intel)
│   ├── OpenTron_1.0.0_aarch64.dmg      (~135 MB - Apple Silicon)
│   └── (native binaries for both archs)
├── Linux
│   ├── opentron_1.0.0_amd64.deb        (~120 MB)
│   ├── opentron_1.0.0_amd64.AppImage   (~140 MB)
│   └── (native binaries)
└── CHECKSUMS.sha256  (for verification)
```

---

## How to Install

### From Local Build

**Using MSI (Recommended):**
```powershell
# Navigate to the bundle directory
cd frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\msi

# Double-click the .msi file
Start-Process -FilePath "OpenTron_1.0.0_x64.msi"

# Or install silently from command line
msiexec /i "OpenTron_1.0.0_x64.msi" /quiet /norestart
```

**Using NSIS Installer:**
```powershell
cd frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\nsis

# Run installer
Start-Process -FilePath "OpenTron_1.0.0_x64_en-US.exe"

# Or install silently
.\OpenTron_1.0.0_x64_en-US.exe /S /D=C:\Program Files\OpenTron
```

### From GitHub Release

1. Go to: https://github.com/your-org/OpenTron/releases
2. Find **v1.0.0**
3. Download **OpenTron_1.0.0_x64.msi** (or .exe)
4. Run the installer
5. Start menu → OpenTron

---

## Verification

After installation:

**Check installed location:**
```powershell
Get-Item "C:\Program Files\OpenTron\*"

# Output:
# Mode     LastWriteTime    Length Name
# ----     -------------    ------ ----
# -a----   7/21/2026  ...   58 MB  opentron-backend-windows-x86_64.exe
# -a----   7/21/2026  ...   2 MB   OpenTron.exe
```

**Verify startup performance:**
```powershell
# Measure app startup time
Measure-Command { Start-Process "C:\Program Files\OpenTron\OpenTron.exe" }

# Should be <2 seconds with native backend
# vs 4-5 seconds with Java fallback
```

**Check process:**
```powershell
# Should show native binary running, not java.exe
Get-Process opentron-backend* -ErrorAction SilentlyContinue

# Output: opentron-backend-windows-x86_64.exe
```

---

## Uninstall

**Via Control Panel:**
- Settings → Apps → Installed apps
- Search "OpenTron"
- Click "Uninstall"

**Via Command Line:**
```powershell
# Using MSI
msiexec /x "OpenTron_1.0.0_x64.msi" /quiet

# Using NSIS
"C:\Program Files\OpenTron\Uninstall.exe" /S
```

---

## Installer File Sizes

| Format | Size | Notes |
|--------|------|-------|
| MSI | 120-150 MB | Better for deployment |
| NSIS EXE | 100-130 MB | Faster installation |
| Portable EXE | 90-110 MB | No install needed |
| Native Binary | 50-60 MB | Runtime only |

---

## Build Script Output

When you run the local build script:

```powershell
.\scripts\build-native-desktop.ps1
```

Expected console output:
```
[1/6] Checking prerequisites...
      ✓ Java 21 found
      ✓ Maven found
      ✓ Node.js found
      ✓ Cargo found

[2/6] Building native backend...
      Compiling GraalVM native-image...
      ✓ Created: java/opentron-java/backend/target/opentron-backend-windows-x86_64.exe (58 MB)

[3/6] Setting up sidecar...
      ✓ Copied native binary to frontend/src-tauri/sidecar/

[4/6] Installing frontend dependencies...
      ✓ npm install complete

[5/6] Building desktop app...
      ✓ Cargo build --release complete

[6/6] Creating installers...
      ✓ MSI: frontend/src-tauri/target/x86_64-pc-windows-msvc/release/bundle/msi/
         └── OpenTron_1.0.0_x64.msi (130 MB)
      ✓ NSIS: frontend/src-tauri/target/x86_64-pc-windows-msvc/release/bundle/nsis/
         └── OpenTron_1.0.0_x64_en-US.exe (110 MB)

BUILD COMPLETE ✓
Total time: 45-70 minutes
```

---

## Troubleshooting

### Installer not found
```powershell
# Check if build output exists
Get-ChildItem "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\" -Recurse

# If empty, run build script:
.\scripts\build-native-desktop.ps1
```

### "Cannot find path" error
```powershell
# Make sure you're in the project root
cd C:\path\to\OpenTron
ls

# Output: backend, frontend, java, scripts, ...
```

### Need to rebuild
```powershell
# Clean previous build
Remove-Item -Recurse -Force "frontend\src-tauri\target" -ErrorAction SilentlyContinue

# Rebuild
.\scripts\build-native-desktop.ps1
```

---

## Next Steps

1. **Build locally:**
   ```powershell
   .\scripts\build-native-desktop.ps1
   ```

2. **Test installer:**
   ```powershell
   Start-Process "frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\msi\OpenTron_1.0.0_x64.msi"
   ```

3. **Verify installation:**
   - Launch from Start menu
   - Check setup screen shows native backend
   - Measure startup time (should be <2 sec)

4. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Build: Windows installers ready"
   git push origin main
   ```

5. **Create release (CI/CD builds all platforms):**
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

---

## Resources

- **MSI Documentation**: https://docs.microsoft.com/en-us/windows/win32/msi/
- **NSIS Installer**: https://nsis.sourceforge.io/
- **Tauri Bundler**: https://tauri.app/v1/guides/building/windows/
- **GraalVM Native Image**: https://www.graalvm.org/latest/reference-manual/native-image/

