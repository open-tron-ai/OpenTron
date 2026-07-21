# GitHub Actions Workflows Reference

## Overview

Three workflows coordinate multi-platform native backend builds and desktop app bundling:

| Workflow | Trigger | Purpose | Output |
|----------|---------|---------|--------|
| `native-backend-build.yml` | Push `java/opentron-java/backend/**` | Build GraalVM native binaries (5 platforms) | Artifacts + Release uploads |
| `desktop-app-build.yml` | Push `frontend/**` | Build desktop apps with bundled natives | App bundles (DMG, AppImage, DEB, EXE) |
| `release.yml` | Git tag `v*.*.*` | Orchestrate all builds, create release | GitHub Release + checksums |

---

## 1. Native Backend Build (`native-backend-build.yml`)

### Triggers
```yaml
on:
  push:
    branches: [main, develop]
    paths:
      - 'java/opentron-java/backend/**'
  pull_request:
    branches: [main, develop]
    paths:
      - 'java/opentron-java/backend/**'
  workflow_dispatch:  # Manual trigger
```

### Matrix Builds (Parallel)

| Runner | OS | Arch | Profile | Time |
|--------|----|----|---------|------|
| ubuntu-latest | Linux | x86_64 | native-linux-x86_64 | 18–20 min |
| ubuntu-latest | Linux | aarch64 | native-linux-aarch64 | 18–20 min |
| macos-13 | macOS | x86_64 | native-macos-x86_64 | 22–25 min |
| macos-14 | macOS | aarch64 | native-macos-aarch64 | 20–23 min |
| windows-latest | Windows | x86_64 | native-windows-x86_64 | 25–30 min |

**Total time:** ~30 min (parallel) vs ~2 hours (sequential)

### Jobs

#### `build-native`
```yaml
- Setup GraalVM JDK 21 (via graalvm/setup-graalvm@v1)
- Verify native-image CLI
- Run: mvn -DskipTests=true -Pnative-<profile> clean package
- Upload artifact: native-binary-{os}-{arch}
- If tagged: Upload to GitHub Release
```

**Artifacts:**
- Retention: 30 days
- Name: `native-binary-{os}-{arch}`
- Contents: `opentron-backend-{platform}` binary

#### `verify-build`
```yaml
- Download all native binaries
- List + verify sizes
- Check for missing artifacts
```

**Purpose:** Ensure all 5 binaries built successfully

### Outputs

**Artifacts (GitHub UI: Actions → Artifacts):**
```
native-binary-linux-x86_64/
  └── opentron-backend-linux-x86_64 (50 MB)
native-binary-linux-aarch64/
  └── opentron-backend-linux-aarch64 (48 MB)
native-binary-macos-x86_64/
  └── opentron-backend-macos-x86_64 (55 MB)
native-binary-macos-aarch64/
  └── opentron-backend-macos-aarch64 (52 MB)
native-binary-windows-x86_64/
  └── opentron-backend-windows-x86_64.exe (58 MB)
```

---

## 2. Desktop App Build (`desktop-app-build.yml`)

### Triggers
```yaml
on:
  push:
    branches: [main, develop]
    paths:
      - 'frontend/**'
      - '.github/workflows/desktop-app-build.yml'
  pull_request:
    branches: [main, develop]
    paths:
      - 'frontend/**'
  workflow_dispatch:
```

### Matrix Builds

| Runner | OS | Arch | Target | Bundle | Time |
|--------|----|----|--------|--------|------|
| macos-13 | macOS | x86_64 | x86_64-apple-darwin | DMG | 18–20 min |
| macos-14 | macOS | aarch64 | aarch64-apple-darwin | DMG | 18–20 min |
| ubuntu-latest | Linux | x86_64 | x86_64-unknown-linux-gnu | AppImage | 15–18 min |
| ubuntu-latest | Linux | aarch64 | aarch64-unknown-linux-gnu | DEB | 15–18 min |
| windows-latest | Windows | x86_64 | x86_64-pc-windows-msvc | EXE/MSI | 20–25 min |

### Jobs

#### `build-desktop`
```yaml
- Setup Node.js 20 + Rust stable
- Install Tauri CLI
- npm ci + npm run build (frontend)
- Download native binaries (previous artifact)
- Organize binaries to sidecar/
- cargo build --release --target <platform>
- Create platform-specific bundle (DMG/AppImage/DEB/EXE)
```

**Steps:**
1. Download `native-binary-{os}-{arch}` artifact
2. Extract to `frontend/src-tauri/sidecar/`
3. Flatten directory structure (remove artifact folders)
4. Build Tauri with bundled native binary
5. Output to `target/{target}/release/bundle/`

**Artifacts:**
- Retention: 30 days
- Name: `app-{os}-{arch}`
- Contents: Bundle folder with platform-specific outputs

#### `sign-macos` (Conditional)
```yaml
if: startsWith(github.ref, 'refs/tags/')
- Import signing certificate (from secrets)
- Sign app binaries
- Upload notarization request (Apple)
- Wait for notarization response
```

**Requires secrets:**
- `MACOS_SIGNING_CERT` (base64-encoded .p12)
- `MACOS_SIGNING_CERT_PASSWORD`

**Note:** Skipped if secrets not configured

#### `test-builds`
```yaml
- Download all app artifacts
- Verify bundle integrity
- List output files
```

**Purpose:** Smoke test for all 5 platforms

### Outputs

**Artifacts:**
```
app-macos-x86_64/
  └── bundle/dmg/
      ├── *.dmg
      └── *.dmg.sig
app-macos-aarch64/
  └── bundle/dmg/
      └── *.dmg
app-linux-x86_64/
  └── bundle/appimage/
      └── *.AppImage
app-linux-aarch64/
  └── bundle/deb/
      └── *.deb
app-windows-x86_64/
  └── bundle/
      ├── msi/*.msi
      └── nsis/*.exe
```

---

## 3. Release Workflow (`release.yml`)

### Triggers
```yaml
on:
  push:
    tags:
      - 'v*.*.*'  # Matches: v1.0.0, v1.0.0-alpha, etc.
  workflow_dispatch:
    inputs:
      version:
        description: 'Release version (e.g., v1.0.0)'
        required: true
```

### Flow

```
Tag pushed (v1.0.0)
    ↓
release.yml triggered
    ├─ call: native-backend-build.yml (needs: build-all)
    │   └─ Builds 5 native binaries
    │
    ├─ call: desktop-app-build.yml (needs: build-desktop)
    │   └─ Builds 5 desktop apps
    │
    └─ create-release job
        ├─ Download all artifacts
        ├─ Organize into release/ folder
        ├─ Generate CHECKSUMS.sha256
        ├─ Create RELEASE_NOTES.md
        └─ Create GitHub Release (softprops/action-gh-release@v1)
```

### Jobs

#### `build-all`
```yaml
uses: ./.github/workflows/native-backend-build.yml
```
Reusable workflow that runs native builds

#### `build-desktop`
```yaml
uses: ./.github/workflows/desktop-app-build.yml
needs: build-all  # Wait for natives first
```
Reusable workflow that runs desktop builds with native binaries

#### `create-release`
```yaml
needs: [build-all, build-desktop]

Steps:
1. Download all artifacts
   └─ 5 native binaries + 5 app bundles
2. Organize into release/ structure
3. Generate SHA256 checksums
4. Create RELEASE_NOTES.md
5. Create GitHub Release with:
   - All native binaries
   - All app bundles
   - CHECKSUMS.sha256
   - RELEASE_NOTES.md
6. Mark as prerelease if tag contains alpha/beta
```

### Release Notes (Auto-Generated)

```markdown
# Release Notes

## Native Backends
- Linux x86_64: opentron-backend-linux-x86_64
- Linux ARM64: opentron-backend-linux-aarch64
- macOS x86_64: opentron-backend-macos-x86_64
- macOS ARM64: opentron-backend-macos-aarch64
- Windows x86_64: opentron-backend-windows-x86_64.exe

## Desktop Apps
- macOS Intel: *.dmg
- macOS ARM64: *.dmg
- Linux AppImage: *.AppImage
- Linux DEB: *.deb
- Windows: *.exe

## Performance
- ✅ 0.5-1.5s startup (native) vs 3-5s (Java)
- ✅ 2-4x faster app boot

## Installation
[Instructions for each platform]

## Checksums
See CHECKSUMS.sha256
```

### Outputs

**GitHub Release includes:**
- ✅ 5 native binaries
- ✅ macOS DMGs (2)
- ✅ Linux AppImage (1)
- ✅ Linux DEB (1)
- ✅ Windows EXE/MSI (2)
- ✅ CHECKSUMS.sha256
- ✅ RELEASE_NOTES.md
- ✅ Release body (auto-populated)

---

## Workflow Dependencies & Parallelization

### Build Order
```
┌─ native-backend-build.yml ─┐
│  (5 jobs in parallel)       │
│  ~30 min total              │
└───────────────────────────┬─┘
                            ↓
        ┌─ desktop-app-build.yml ─┐
        │  (5 jobs in parallel)    │
        │  ~25 min total           │
        │  (downloads natives)     │
        └──────────────────────┬──┘
                               ↓
                   ┌─ create-release ─┐
                   │ (wait for above)  │
                   │ ~5 min            │
                   └──────────────────┘
```

**Total time:** ~60–65 min (all parallel)

### Reusable Workflows

Both `native-backend-build.yml` and `desktop-app-build.yml` are **reusable**:

```yaml
# In release.yml:
jobs:
  build-all:
    uses: ./.github/workflows/native-backend-build.yml
  
  build-desktop:
    uses: ./.github/workflows/desktop-app-build.yml
    needs: build-all
```

Benefits:
- ✅ DRY (Don't Repeat Yourself)
- ✅ Single source of truth for build logic
- ✅ Can be triggered independently or from release workflow

---

## Environment Variables & Secrets

### Built-in (GitHub)
- `GITHUB_TOKEN` — Auto-provided, used by softprops/action-gh-release@v1
- `GITHUB_REF` — Tag or branch (e.g., `refs/tags/v1.0.0`)

### Optional Secrets (for signing)
- `MACOS_SIGNING_CERT` — Base64-encoded .p12 file
- `MACOS_SIGNING_CERT_PASSWORD` — Cert password

### GitHub Action Versions
- `actions/checkout@v4` — Clone repo
- `actions/setup-node@v4` — Node.js
- `actions-rs/toolchain@v1` — Rust
- `graalvm/setup-graalvm@v1` — GraalVM JDK
- `actions/upload-artifact@v4` — Store artifacts
- `actions/download-artifact@v4` — Retrieve artifacts
- `softprops/action-gh-release@v1` — Create release

---

## Caching Strategy

### Maven Cache
```yaml
- uses: actions/setup-java@v4
  with:
    distribution: 'graalvm'
    java-version: '21'
    cache: maven  # ← Auto-caches ~/.m2
```

**Benefit:** ~5–10 min saved on native builds

### Cargo Cache
```yaml
- uses: actions-rs/toolchain@v1
  with:
    cache: true  # ← Auto-caches ~/.cargo
```

**Benefit:** ~3–5 min saved on desktop builds

### npm Cache
```yaml
- uses: actions/setup-node@v4
  with:
    cache: 'npm'
    cache-dependency-path: 'frontend/package-lock.json'
```

**Benefit:** ~1–2 min saved

---

## Failure Handling

### Retry on Transient Failure
```yaml
- name: Build Native
  run: mvn clean package
  continue-on-error: true  # Don't fail job immediately
```

Not used in current workflows, but can be added if builds are flaky.

### Skip Jobs Conditionally
```yaml
jobs:
  sign-macos:
    if: secrets.MACOS_SIGNING_CERT != ''
```

Skips if signing cert not configured (no-op).

### Artifact Warnings
```yaml
if-no-files-found: warn  # Don't fail if artifact empty
```

Artifact upload warns but doesn't fail job.

---

## Monitoring & Logging

### View Workflow Status
1. GitHub → **Actions** tab
2. Select workflow name
3. See all runs with status (✅ success, ✗ failed, ⏳ in progress)

### View Job Logs
1. Click run name
2. Click job name
3. Expand steps for detailed logs
4. Search for errors or warnings

### Download Artifacts
1. Completed workflow run
2. **Artifacts** section
3. Click to download (zip format)

---

## Advanced: Custom Modifications

### Add Parallel Test Job
```yaml
test:
  runs-on: ubuntu-latest
  needs: build-native
  steps:
    - uses: actions/checkout@v4
    - name: Download Artifacts
      uses: actions/download-artifact@v4
    - name: Test Binaries
      run: ./test-native-binaries.sh
```

### Publish to Package Registry
```yaml
- name: Publish to Docker Hub
  run: docker push opentron-backend:${{ github.ref_name }}
```

### Notify on Completion
```yaml
- name: Slack Notification
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {"text": "Release ${{ github.ref_name }} published!"}
```

---

## Troubleshooting Workflows

### Workflow Not Triggering
- Check **Settings → Actions** — ensure Actions enabled
- Verify branch/tag in `on:` condition matches
- Re-push commit: `git push origin --force`

### Artifact Not Found
- Check job completed successfully (green ✅)
- Verify artifact upload step ran
- Check `if-no-files-found: warn` or `error`

### Build Timeout (exceeded 6 hours)
- GitHub max job time: 6 hours
- Current workflows: ~1 hour total
- If exceeds, split into smaller jobs or use self-hosted runners

### GraalVM Build OOM (Out of Memory)
- Increase runner size (GitHub: fixed at 7 GB)
- Or use self-hosted runner with more RAM
- Or split native builds into smaller modules

---

## Cost Considerations

### GitHub Actions Pricing
- **Public repos:** Free (unlimited minutes)
- **Private repos:** 2,000 minutes/month free tier
- **Extra:** $0.25 per minute on macOS/Windows runners

### Release Workflow Cost
- 5 native builds: ~120 min (linux/windows standard, macos standard)
- 5 desktop builds: ~110 min
- **Total:** ~230 min (~$0.50 on private repo)

### Optimize:
- Use Linux runners when possible (cheapest)
- Cache aggressively
- Combine jobs where feasible
- Use self-hosted runners for frequent releases

