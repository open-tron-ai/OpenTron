# How to Create a GitHub Release

## Quick Start

### Option 1: Using the Script (Recommended)

**Windows (PowerShell):**
```powershell
.\scripts\create-release.ps1 -Version v1.0.0
```

**macOS/Linux (Bash):**
```bash
chmod +x scripts/create-release.sh
./scripts/create-release.sh v1.0.0
```

### Option 2: Manual Git Commands

```bash
# Create a tag
git tag -a v1.0.0 -m "Release v1.0.0"

# Push the tag to GitHub
git push origin v1.0.0
```

### Option 3: GitHub Web UI

1. Go to GitHub → Code → Releases
2. Click "Create a new release"
3. Choose tag: `v1.0.0`
4. Release title: `OpenTron v1.0.0`
5. Description: (auto-filled from RELEASE_NOTES.md)
6. Click "Publish release"

---

## What Happens After Creating Release

### Automated Workflow Triggers

When you push a tag matching `v*.*.*`:

1. **release.yml** workflow starts
   - Creates GitHub Release page
   - Generates release notes
   - Status: `Releases` tab

2. **native-backend-build.yml** workflow runs
   - Builds 5 native binaries (parallel):
     - Linux x86_64 (15-20 min)
     - Linux ARM64 (15-20 min)
     - macOS x86_64 (20-25 min)
     - macOS ARM64 (20-25 min)
     - Windows x86_64 (25-30 min)
   - Status: `Actions` → `Native Backend Build` tab

3. **desktop-app-build.yml** workflow runs
   - Builds 5 desktop apps (parallel):
     - macOS DMG (Intel)
     - macOS DMG (Apple Silicon)
     - Linux AppImage
     - Linux DEB
     - Windows EXE/MSI
   - Status: `Actions` → `Desktop App Build` tab

4. **upload-release-assets.yml** workflow (optional)
   - Automatically uploads artifacts to release
   - Status: `Releases` → `v1.0.0` → Assets

---

## Timeline

```
T+0 min      You push tag: git push origin v1.0.0
             ↓
T+1 min      GitHub Actions triggered
             ├─ release.yml: Create Release page (1-2 min)
             ├─ native-backend-build.yml: Start 5 builds (parallel)
             └─ desktop-app-build.yml: Start 5 builds (parallel)
             ↓
T+30 min     Native builds complete
             ├─ opentron-backend-linux-x86_64
             ├─ opentron-backend-linux-aarch64
             ├─ opentron-backend-macos-x86_64
             ├─ opentron-backend-macos-aarch64
             └─ opentron-backend-windows-x86_64.exe
             ↓
T+50 min     Desktop apps complete
             ├─ macOS DMG files
             ├─ Linux AppImage/DEB files
             └─ Windows EXE/MSI files
             ↓
T+55 min     Assets uploaded to release (optional)
             ↓
DONE ✅      Release ready with all artifacts
```

---

## Verify Release Was Created

### Check GitHub Web UI
1. GitHub → Releases
2. Should show `v1.0.0` at top
3. Status: "Latest release" (green tag)

### Check Command Line
```bash
# List releases
gh release list --limit 5

# View specific release
gh release view v1.0.0
```

### Expected Release Contents

After all workflows complete, release should contain:

```
📦 OpenTron v1.0.0

Files:
├── 📄 RELEASE_NOTES.md
├── 🔐 CHECKSUMS.sha256
├── Native Binaries (50-60 MB each)
│   ├── opentron-backend-linux-x86_64
│   ├── opentron-backend-linux-aarch64
│   ├── opentron-backend-macos-x86_64
│   ├── opentron-backend-macos-aarch64
│   └── opentron-backend-windows-x86_64.exe
├── macOS
│   ├── OpenTron_1.0.0_x64.dmg (130 MB)
│   └── OpenTron_1.0.0_aarch64.dmg (130 MB)
├── Linux
│   ├── opentron_1.0.0_amd64.AppImage (140 MB)
│   └── opentron_1.0.0_aarch64.deb (120 MB)
└── Windows
    ├── OpenTron_1.0.0_x64.exe (110 MB)
    └── OpenTron_1.0.0_x64.msi (130 MB)
```

---

## Troubleshooting

### Release created but no artifacts

**Problem:** Release page exists but no files uploaded.

**Cause:** Build workflows haven't completed yet.

**Solution:** 
1. Check `Actions` tab for workflow status
2. Wait 30-50 minutes for builds to complete
3. Assets upload automatically when builds finish

### "Tag already exists" error

**Problem:** Cannot push tag because it exists locally.

**Solution:**
```bash
# Delete local tag
git tag -d v1.0.0

# Or push with force (not recommended)
git push origin v1.0.0 --force
```

### Release page shows but workflows not running

**Problem:** Pushed tag but workflows didn't trigger.

**Cause:** Workflows may have syntax errors.

**Solution:**
1. Check `.github/workflows/release.yml` syntax
2. Check `Actions` → `All workflows` for errors
3. Manually re-run workflow (Actions tab)

### No permissions to create release

**Problem:** "Permission denied" when running script.

**Cause:** Script lacks execute permission.

**Solution:**
```bash
chmod +x scripts/create-release.sh
./scripts/create-release.sh v1.0.0
```

### Need to upload artifacts manually

**Problem:** Workflows completed but assets not on release.

**Reason:** `upload-release-assets.yml` may not have run.

**Solution:**
```bash
# Upload native binary
gh release upload v1.0.0 java/opentron-java/backend/target/opentron-backend-windows-x86_64.exe

# Upload desktop app
gh release upload v1.0.0 frontend/src-tauri/target/x86_64-pc-windows-msvc/release/bundle/msi/*.msi
```

---

## Next Steps

After release is created and artifacts are available:

1. **Test Installation**
   - Download .msi/.exe from release
   - Install and verify startup time (<2 sec)

2. **Test on Other Platforms**
   - Download macOS .dmg
   - Download Linux AppImage

3. **Announce Release**
   - Share release link
   - Highlight 2-4x performance improvement

4. **For Next Release**
   ```bash
   git tag -a v1.1.0 -m "Release v1.1.0: Bug fixes"
   git push origin v1.1.0
   ```

---

## Files Created/Modified

- `.github/workflows/release.yml` — Fixed to create release
- `.github/workflows/upload-release-assets.yml` — Auto-upload artifacts
- `scripts/create-release.sh` — Bash helper script
- `scripts/create-release.ps1` — PowerShell helper script

