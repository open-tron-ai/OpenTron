# Quick Start: CI/CD for GraalVM Native Binaries & Desktop Apps

## TL;DR

```bash
# 1. Push code to main/develop branch
git push origin main

# 2. GitHub Actions automatically:
#    - Builds 5 native binaries (all platforms)
#    - Builds 5 desktop apps (macOS/Linux/Windows)
#    - Stores artifacts for 30 days

# 3. To create a release:
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# 4. GitHub Release page auto-populates with:
#    - All native binaries
#    - All desktop app bundles (DMG, AppImage, DEB, EXE)
#    - Checksums + release notes
```

---

## Local Testing (Without GitHub)

### macOS / Linux
```bash
# Build native backend + desktop app for your platform
chmod +x scripts/build-native-desktop.sh
./scripts/build-native-desktop.sh

# Output: frontend/src-tauri/target/*/release/bundle/
```

### Windows
```batch
# Build native backend + desktop app for Windows
scripts\build-native-desktop.bat

# Output: frontend\src-tauri\target\x86_64-pc-windows-msvc\release\bundle\
```

**Time:** ~30–45 minutes (first build, cached after)

---

## Monitoring CI/CD Runs

### GitHub Actions Dashboard
1. Go to **GitHub → Your Repo → Actions**
2. Click on workflow name (e.g., "Native Backend Build")
3. See real-time build logs for all platforms

### View Artifacts
1. Go to **Actions → Completed workflow run**
2. Click **Artifacts** section
3. Download native binaries or desktop apps

**Retention:** 30 days (auto-deleted after)

---

## Release Checklist

### Before Tagging
- [ ] Version bumped in:
  - `frontend/src-tauri/tauri.conf.json` (check `version` field)
  - `java/opentron-java/backend/pom.xml` (check `<version>`)
  - `frontend/package.json`
- [ ] Changelog updated (`CHANGELOG.md`)
- [ ] All tests passing (`main` branch)
- [ ] Commits merged to `main`

### Create Release
```bash
# From main branch
git tag -a v1.0.0 -m "Release v1.0.0

## Features
- GraalVM native backend (2-4x faster startup)
- Multi-platform desktop apps
- Live setup screen with boot progress

## Downloads
See GitHub Release page for all binaries and checksums."

git push origin v1.0.0
```

### After Tag Push
1. GitHub Actions runs automatically (3–4 hours)
2. Check **Actions** tab for progress
3. Release page populates at **Releases → Latest**
4. Download + test binaries

---

## Artifacts Generated

### Native Binaries
```
opentron-backend-linux-x86_64      (~50 MB)
opentron-backend-linux-aarch64     (~48 MB)
opentron-backend-macos-x86_64      (~55 MB)
opentron-backend-macos-aarch64     (~52 MB)
opentron-backend-windows-x86_64.exe (~58 MB)
```

### Desktop Apps
```
macOS/          Intel + ARM64 DMGs
Linux/          AppImage (x86_64) + DEB (ARM64)
Windows/        EXE + MSI installers
```

### Release Files
```
CHECKSUMS.sha256    (SHA256 hashes for verification)
RELEASE_NOTES.md    (Auto-generated with all details)
```

---

## Testing Downloaded Artifacts

### Verify Checksums
```bash
# Download CHECKSUMS.sha256 from release
sha256sum -c CHECKSUMS.sha256

# Should show: OK for all files
```

### Test Native Binary
```bash
# Linux / macOS
chmod +x opentron-backend-linux-x86_64
./opentron-backend-linux-x86_64

# Windows
opentron-backend-windows-x86_64.exe
```

Should start and listen on port 8000.

### Test Desktop App
- **macOS:** Double-click `.dmg` → Drag to Applications → Launch
- **Linux:** `chmod +x *.AppImage && ./*.AppImage` or `sudo dpkg -i *.deb`
- **Windows:** Double-click `.exe` or `.msi`

Should show setup screen → "Starting native API server..." → Launch main app

---

## Troubleshooting

### Workflow Failed
1. Go to **Actions** → Failed run
2. Expand failed step (red ✗)
3. Check error message
4. Common fixes:
   - **GraalVM error:** Ensure `graalvm/setup-graalvm@v1` is first step
   - **Maven error:** Check Java version (must be 21+)
   - **Tauri error:** Run locally first with `./scripts/build-native-desktop.sh`

### Artifact Not Found
1. Check artifact uploaded:
   - **Actions** → Run details → **Artifacts** section
   - If empty, build step may have failed
2. Re-trigger workflow with **"Run workflow"** button

### Release Page Empty
1. Tag push may not have triggered workflow
2. Try manual trigger: **Actions** → **Release** → **Run workflow**
3. Or re-push tag: `git push origin v1.0.0 --force`

---

## Optimization Tips

### Speed Up Native Builds
- ✅ Already cached (Maven, Cargo)
- 🔄 Parallel jobs (5 platforms build simultaneously)
- 📦 Reduce binary size with native-image options:
  ```xml
  <configuration>
    <imageName>opentron-backend</imageName>
    <buildArgs>-H:+RemoveUnusedSymbols</buildArgs>
  </configuration>
  ```

### Speed Up Desktop Builds
- ✅ Already cached (Node, Cargo)
- 📉 Skip code signing for non-release builds
- 🔄 Run tests in parallel with artifact downloads

### Monitor Costs
- GitHub Actions: ~120 min per release (5 builds × 20–30 min)
- Free tier: 2,000 minutes/month (plenty for small team)
- If needed: Switch runners or use self-hosted

---

## Advanced: Custom Builds

### Build Only Native (Skip Desktop)
```bash
# Trigger native-backend-build.yml manually
# Or push to java/opentron-java/backend/**
```

### Build Only Desktop
```bash
# Manually copy native binary to frontend/src-tauri/sidecar/
# Push to frontend/**
# Desktop build uses cached native binary
```

### Cross-Compile ARM64 on x86
```bash
# Use Docker in CI (requires Buildx setup)
# Or use dedicated ARM runner from GitHub
```

---

## Next Releases

After v1.0.0, repeat for each new release:

1. Bump versions
2. Update changelog
3. Commit + push to `main`
4. Tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
5. Push tag: `git push origin vX.Y.Z`
6. Wait 3–4 hours
7. Verify release page + test artifacts

---

## Support

- 📖 Full docs: See `CI_CD_SETUP.md`
- 🐛 Issues: Check GitHub Actions logs
- 💬 Questions: File issue with `ci/cd` label

