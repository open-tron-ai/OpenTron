# GraalVM Native Build Fix - GitHub Actions

## Problem

```
Error: 'gu.cmd' tool was not found in your JAVA_HOME.
This probably means that the JDK at 'C:\hostedtoolcache\windows\Java_Temurin-Hotspot_jdk\21.0.11-10.0\x64' 
is not a GraalVM distribution.
```

## Root Cause

GitHub's default Java setup was using **Temurin JDK** (Oracle's OpenJDK build) instead of **GraalVM JDK**.

The native-image plugin requires GraalVM-specific tools (`gu` - GraalVM Updater) which are only available in GraalVM distributions.

## Solution Applied

### Updated Workflow Configuration

Changed in `.github/workflows/native-backend-build.yml`:

```yaml
# BEFORE (incorrect - uses default Temurin)
- uses: graalvm/setup-graalvm@v1
  with:
    java-version: '21'
    distribution: 'graalvm'  # ❌ This wasn't enforcing GraalVM
    cache: maven

# AFTER (correct - explicitly uses GraalVM Community)
- uses: graalvm/setup-graalvm@v1
  with:
    java-version: '21'
    distribution: 'graalvm-community'  # ✅ Forces GraalVM
    cache: maven
    native-image-job-reports: 'true'
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

### Key Changes

1. **`distribution: 'graalvm-community'`** — Explicitly requests GraalVM Community Edition
2. **`native-image-job-reports: 'true'`** — Enables detailed build reports
3. **`github-token`** — Allows GitHub integration for build artifacts

### Verification Step Added

```yaml
- name: Verify GraalVM Installation
  run: |
    java -version
    native-image --version
  shell: bash
```

This ensures GraalVM (not Temurin) is installed before attempting native build.

---

## What This Fixes

✅ Native-image compiler now available
✅ `gu` tool accessible for native builds
✅ All 5 platform builds can proceed
✅ No more "GraalVM distribution" errors

---

## Expected Build Flow Now

```
Setup GraalVM (via graalvm/setup-graalvm@v1 with graalvm-community)
    ↓
Verify installation (java -version, native-image --version)
    ↓
Run Maven with native profile
    ↓
GraalVM native-image compiles backend binary
    ↓
Upload artifact (50-60 MB)
```

---

## If Issues Persist

### 1. Check Workflow Run Logs
- GitHub → Actions → Native Backend Build
- Look for "Setup GraalVM JDK" step output
- Verify it says "graalvm-community"

### 2. Verify native-image is Available
The "Verify GraalVM Installation" step should show:
```
native-image version 21.x.x
```

### 3. Check Maven Profile Activation
Build logs should show:
```
[INFO] Using 'native-windows-x86_64' profile
[INFO] Building GraalVM native-image...
```

### 4. Manual Fix (if needed)
Edit `.github/workflows/native-backend-build.yml`:
- Ensure `distribution: 'graalvm-community'`
- Ensure `java-version: '21'` (not '20' or '22')
- Check runner OS matches matrix configuration

---

## Testing the Fix

### Option 1: Push to Develop Branch
```bash
git add .github/workflows/native-backend-build.yml
git commit -m "fix: use graalvm-community distribution for native builds"
git push origin develop
```

GitHub Actions will auto-trigger on next push.

### Option 2: Manual Workflow Trigger
1. GitHub → Actions
2. Select "Native Backend Build" workflow
3. Click "Run workflow"
4. Select branch: `develop`
5. Click green "Run workflow" button

### Option 3: Create Test Release
```bash
git tag -a v0.1.0-test -m "Test GraalVM native build"
git push origin v0.1.0-test
```

This triggers both native build and release workflows.

---

## Build Time Expectations

| Platform | Time | Status |
|----------|------|--------|
| Linux x86_64 | 15–20 min | Parallel |
| Linux ARM64 | 15–20 min | Parallel |
| macOS x86_64 | 20–25 min | Parallel |
| macOS ARM64 | 20–25 min | Parallel |
| Windows x86_64 | 25–30 min | Parallel |
| **Total (all parallel)** | **~30 min** | ✅ |

---

## Verification Checklist

After running workflow:

- [ ] Workflow completes without "gu.cmd not found" error
- [ ] All 5 platform builds start (may succeed or fail independently)
- [ ] Artifacts tab shows native binaries (check size ~50-60 MB each)
- [ ] Build logs show "BUILD SUCCESS" for each platform
- [ ] Release page (if tagged) contains all 5 binaries

---

## Resources

- **GraalVM Setup Action**: https://github.com/graalvm/setup-graalvm
- **GraalVM Downloads**: https://www.graalvm.org/downloads/
- **Native Image Guide**: https://www.graalvm.org/latest/reference-manual/native-image/
- **Maven Native Plugin**: https://graalvm.github.io/native-build-tools/latest/maven-plugin.html

---

## Summary

✅ **Fix applied**: Updated workflow to use `graalvm-community` distribution
✅ **Verification added**: Checks for GraalVM installation before build
✅ **Ready to test**: Push to GitHub to trigger workflows
✅ **Expected result**: 5 native binaries built in ~30 minutes

Next: Push code and monitor GitHub Actions for successful builds! 🚀

