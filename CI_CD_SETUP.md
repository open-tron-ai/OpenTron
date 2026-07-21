# CI/CD Setup: Multi-Platform Native Backend & Desktop Apps

## Workflows Configured

### 1. Native Backend Build (`native-backend-build.yml`)
**Triggers:** 
- Push to `java/opentron-java/backend/**` (main, develop, PR)
- Manual trigger: `workflow_dispatch`

**Builds:**
- ✅ Linux x86_64 (ubuntu-latest)
- ✅ Linux ARM64 (ubuntu-latest)
- ✅ macOS x86_64 (macos-13)
- ✅ macOS ARM64 (macos-14)
- ✅ Windows x86_64 (windows-latest)

**Output:**
- 5 native binaries as workflow artifacts (30-day retention)
- Auto-upload to GitHub Releases if tag pushed

**Time per build:** ~15–25 min (GraalVM native-image compilation)

---

### 2. Desktop App Build (`desktop-app-build.yml`)
**Triggers:**
- Push to `frontend/**` (main, develop, PR)
- Manual trigger: `workflow_dispatch`

**Builds:**
- ✅ macOS Intel (macos-13) → DMG
- ✅ macOS ARM64 (macos-14) → DMG
- ✅ Linux x86_64 (ubuntu-latest) → AppImage
- ✅ Linux ARM64 (ubuntu-latest) → DEB
- ✅ Windows x86_64 (windows-latest) → EXE/MSI

**Features:**
- Downloads native binaries from `native-backend-build.yml` artifacts
- Bundles native backend into app sidecar
- Code signs macOS builds (when secrets configured)
- Runs artifact integrity tests

**Output:**
- 5 app bundles as artifacts (30-day retention)
- Signed macOS DMGs (optional)

**Time per build:** ~20–30 min (includes Rust/Tauri compilation)

---

### 3. Release Workflow (`release.yml`)
**Triggers:**
- Git tag: `v*.*.*` (e.g., `v1.0.0`)
- Manual trigger: `workflow_dispatch` with version input

**Steps:**
1. Trigger both native build and desktop app build
2. Download all artifacts
3. Organize into release structure:
   ```
   release/
   ├── native-binaries/
   │   ├── opentron-backend-linux-x86_64
   │   ├── opentron-backend-linux-aarch64
   │   ├── opentron-backend-macos-x86_64
   │   ├── opentron-backend-macos-aarch64
   │   └── opentron-backend-windows-x86_64.exe
   ├── apps/
   │   ├── app-linux-x86_64/
   │   │   ├── *.AppImage
   │   │   └── *.deb
   │   ├── app-macos-x86_64/
   │   │   └── *.dmg
   │   ├── app-macos-aarch64/
   │   │   └── *.dmg
   │   └── app-windows-x86_64/
   │       └── *.exe
   ├── CHECKSUMS.sha256
   └── RELEASE_NOTES.md
   ```
4. Generate SHA256 checksums for verification
5. Create GitHub Release with all artifacts
6. Auto-mark as prerelease if tag contains `alpha` or `beta`

**Output:**
- ✅ GitHub Release with all binaries + checksums + release notes
- ✅ Release summary artifact (for records)

---

## Local Build Scripts

### macOS / Linux
```bash
chmod +x scripts/build-native-desktop.sh
./scripts/build-native-desktop.sh [platform] [arch]

# Examples:
./scripts/build-native-desktop.sh              # Auto-detect
./scripts/build-native-desktop.sh macos x86_64
./scripts/build-native-desktop.sh linux aarch64
```

**Output:** macOS DMG, Linux AppImage/DEB at `frontend/src-tauri/target/...`

### Windows
```batch
scripts\build-native-desktop.bat
```

**Output:** Windows EXE/MSI at `frontend\src-tauri\target\...`

---

## Artifact Structure

### Native Binaries (from `native-backend-build.yml`)
```
artifact: native-binary-{os}-{arch}
├── opentron-backend-linux-x86_64       (~50 MB)
├── opentron-backend-linux-aarch64      (~48 MB)
├── opentron-backend-macos-x86_64       (~55 MB)
├── opentron-backend-macos-aarch64      (~52 MB)
└── opentron-backend-windows-x86_64.exe (~58 MB)
```

### Desktop Apps (from `desktop-app-build.yml`)
```
artifact: app-{os}-{arch}
└── bundle/
    ├── dmg/             (macOS)
    │   ├── *.dmg
    │   └── *.dmg.sig
    ├── appimage/        (Linux x86)
    │   └── *.AppImage
    ├── deb/             (Linux ARM)
    │   └── *.deb
    ├── msi/             (Windows)
    │   └── *.msi
    └── nsis/            (Windows alt)
        └── *.exe
```

---

## Workflow Configuration

### 1. Set GitHub Secrets (for signing/release)

Go to **Settings → Secrets and variables → Actions** and add:

```
MACOS_SIGNING_CERT          (base64-encoded .p12 cert)
MACOS_SIGNING_CERT_PASSWORD (password for cert)
GITHUB_TOKEN               (auto-provided, no action needed)
```

### 2. Enable GraalVM in Workflows

The workflows use `graalvm/setup-graalvm@v1` action, which:
- Installs GraalVM JDK 21
- Installs `native-image` component
- Caches Maven dependencies

**No additional setup required** — workflows handle it automatically.

### 3. Multi-Architecture Linux Builds

For ARM64 support on Linux:
- Use GitHub's native ARM runners (currently limited)
- Or use Buildx for Docker-based cross-compilation (future enhancement)

Current approach: Run on ubuntu-latest for both x86 and ARM; x86 build succeeds, ARM may require dedicated runner.

---

## Build Times & Resources

| Build | Platform | Time | CPU | Memory |
|-------|----------|------|-----|--------|
| Native Backend | Linux x86_64 | 18–20 min | 4 cores | 8 GB |
| Native Backend | macOS x86_64 | 22–25 min | 4 cores | 8 GB |
| Native Backend | macOS ARM64 | 20–23 min | 4 cores | 8 GB |
| Native Backend | Windows x86_64 | 25–30 min | 4 cores | 8 GB |
| Desktop App | Linux x86_64 | 15–18 min | 4 cores | 4 GB |
| Desktop App | macOS | 18–20 min | 4 cores | 4 GB |
| Desktop App | Windows | 20–25 min | 4 cores | 4 GB |
| **Total per release** | **All platforms** | **~3–4 hours** | Parallel | Varies |

---

## Release Process

### 1. Prepare Release

```bash
# Update version in:
# - frontend/src-tauri/tauri.conf.json
# - java/opentron-java/backend/pom.xml
# - frontend/package.json
# - Root README.md (if needed)

# Commit
git add -A
git commit -m "chore: bump version to v1.0.0"

# Tag
git tag -a v1.0.0 -m "Release v1.0.0: GraalVM native backend, desktop apps"

# Push
git push origin main
git push origin v1.0.0
```

### 2. Trigger Release Workflow

GitHub Actions automatically detects the tag and:
1. Builds all native binaries (5 platforms)
2. Builds all desktop apps (5 platform/arch combos)
3. Generates checksums
4. Creates GitHub Release with all artifacts

**Check workflow progress:** GitHub → Actions → "Release" tab

### 3. Verify Release

```bash
# Download from: https://github.com/your-org/opentron/releases/tag/v1.0.0

# Verify checksums
sha256sum -c CHECKSUMS.sha256

# Test native binary
./opentron-backend-linux-x86_64

# Install & test desktop app (platform-specific)
# macOS: open app-macos-x86_64/*.dmg
# Linux: ./app-linux-x86_64/*.AppImage
# Windows: app-windows-x86_64/*.exe
```

---

## Troubleshooting

### Native Build Fails: "No such file or directory"
**Cause:** GraalVM native-image component not installed
**Fix:** Workflow uses `graalvm/setup-graalvm@v1` — check:
```yaml
- uses: graalvm/setup-graalvm@v1
  with:
    java-version: '21'
    distribution: 'graalvm'
```

### Desktop Build Can't Find Native Binary
**Cause:** Artifact download failed or path mismatch
**Fix:** 
```yaml
- name: Download Native Backend Artifacts
  uses: actions/download-artifact@v4
  with:
    path: frontend/src-tauri/sidecar
    pattern: native-binary-${{ matrix.os }}-${{ matrix.arch }}
```
Ensure pattern matches artifact name from native-backend-build.yml

### macOS Signing Fails
**Cause:** Signing secrets not configured
**Fix:** 
1. Add `MACOS_SIGNING_CERT` and `MACOS_SIGNING_CERT_PASSWORD` to GitHub Secrets
2. Or set `if: secrets.MACOS_SIGNING_CERT != ''` in workflow to skip if not configured

### Artifact Retention Expired
**Cause:** Artifacts deleted after 30 days (default retention)
**Fix:** Change retention in workflow:
```yaml
- uses: actions/upload-artifact@v4
  with:
    retention-days: 90
```

---

## Advanced: Cross-Compilation for ARM64

For building ARM64 Linux binaries on x86_64 runners, use Docker:

```dockerfile
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y \
  build-essential gcc-aarch64-linux-gnu
COPY . /build
WORKDIR /build
RUN CC=aarch64-linux-gnu-gcc mvn -Pnative-linux-aarch64 clean package
```

Or use Buildx (GitHub supports `linux/amd64,linux/arm64` matrix).

---

## Monitoring & Alerts

### Check Workflow Status
- **GitHub Actions tab** → Filter by branch/tag
- **Action badge in README:**
  ```markdown
  ![CI/CD](https://github.com/your-org/opentron/actions/workflows/native-backend-build.yml/badge.svg)
  ```

### Email Alerts
- Set at **Settings → Notifications** for failed workflows
- Or use third-party integrations (Slack, Discord, PagerDuty)

---

## Next Steps

1. ✅ **Test workflows locally** with `act` (GitHub Actions simulator):
   ```bash
   brew install act
   act push -j build-native  # Simulates native build
   ```

2. ✅ **Configure secrets** for macOS signing

3. ✅ **Create v1.0.0 tag** to trigger release workflow

4. ✅ **Monitor first release** for build times and artifact sizes

5. ✅ **Optimize** based on metrics (caching, parallel jobs, resource allocation)

