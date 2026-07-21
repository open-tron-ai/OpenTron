#!/usr/bin/env bash
# Local build script: Compile native backend and bundle into desktop app
# Usage: ./scripts/build-native-desktop.sh [platform] [arch]
# Platforms: linux, macos, windows (auto-detected if not specified)

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Detect platform if not specified
PLATFORM=${1:-$(uname -s | tr '[:upper:]' '[:lower:]')}
ARCH=${2:-$(uname -m)}

# Normalize arch names
if [[ "$ARCH" == "x86_64" ]]; then
  ARCH="x86_64"
elif [[ "$ARCH" == "aarch64" || "$ARCH" == "arm64" ]]; then
  ARCH="aarch64"
fi

# Normalize platform names
case "$PLATFORM" in
  darwin)
    PLATFORM="macos"
    ;;
  linux)
    PLATFORM="linux"
    ;;
  mingw64_nt-* | windows* | msys*)
    PLATFORM="windows"
    ;;
esac

echo -e "${BLUE}=== OpenTron Native Desktop Builder ===${NC}"
echo -e "Platform: ${YELLOW}${PLATFORM}${NC} (${YELLOW}${ARCH}${NC})"
echo ""

# Validate platform
if [[ ! "$PLATFORM" =~ ^(linux|macos|windows)$ ]]; then
  echo -e "${RED}Error: Unsupported platform: $PLATFORM${NC}"
  echo "Supported platforms: linux, macos, windows"
  exit 1
fi

# Step 1: Check prerequisites
echo -e "${BLUE}[1/5]${NC} Checking prerequisites..."

check_cmd() {
  if ! command -v $1 &> /dev/null; then
    echo -e "${RED}✗ $1 not found${NC}"
    return 1
  fi
  echo -e "${GREEN}✓${NC} $1"
}

check_cmd "java"
check_cmd "mvn"
check_cmd "node"
check_cmd "npm"
check_cmd "cargo"
check_cmd "rustup"

if ! java -version 2>&1 | grep -q "21"; then
  echo -e "${YELLOW}⚠ Warning: Java 21 not detected. GraalVM native build may fail.${NC}"
fi

# Step 2: Build native backend
echo ""
echo -e "${BLUE}[2/5]${NC} Building native backend for ${YELLOW}${PLATFORM}-${ARCH}${NC}..."

cd java/opentron-java/backend

NATIVE_PROFILE=""
case "$PLATFORM-$ARCH" in
  linux-x86_64)
    NATIVE_PROFILE="native-linux-x86_64"
    ;;
  linux-aarch64)
    NATIVE_PROFILE="native-linux-aarch64"
    ;;
  macos-x86_64)
    NATIVE_PROFILE="native-macos-x86_64"
    ;;
  macos-aarch64)
    NATIVE_PROFILE="native-macos-aarch64"
    ;;
  windows-x86_64)
    NATIVE_PROFILE="native-windows-x86_64"
    ;;
esac

if [[ -z "$NATIVE_PROFILE" ]]; then
  echo -e "${YELLOW}⚠ No specific profile for ${PLATFORM}-${ARCH}, using auto-detection${NC}"
  mvn -DskipTests=true clean package
else
  echo -e "Profile: ${YELLOW}${NATIVE_PROFILE}${NC}"
  mvn -DskipTests=true -P${NATIVE_PROFILE} clean package
fi

# Find built binary
NATIVE_BINARY=$(find target -maxdepth 1 -name "opentron-backend*" -type f | head -1)
if [[ -z "$NATIVE_BINARY" ]]; then
  echo -e "${RED}✗ Native binary not found in target/${NC}"
  exit 1
fi

BINARY_SIZE=$(du -h "$NATIVE_BINARY" | cut -f1)
echo -e "${GREEN}✓ Native binary built: ${BINARY_SIZE}${NC}"

cd - > /dev/null

# Step 3: Prepare frontend sidecar
echo ""
echo -e "${BLUE}[3/5]${NC} Setting up frontend sidecar..."

mkdir -p frontend/src-tauri/sidecar
cp "$NATIVE_BINARY" frontend/src-tauri/sidecar/

echo -e "${GREEN}✓${NC} Native binary copied to sidecar"

# Step 4: Build frontend
echo ""
echo -e "${BLUE}[4/5]${NC} Building frontend..."

cd frontend

if [[ ! -d "node_modules" ]]; then
  npm ci
fi

npm run build
echo -e "${GREEN}✓${NC} Frontend built"

cd - > /dev/null

# Step 5: Build desktop app
echo ""
echo -e "${BLUE}[5/5]${NC} Building Tauri desktop app..."

cd frontend/src-tauri

case "$PLATFORM" in
  macos)
    TARGET="aarch64-apple-darwin"
    if [[ "$ARCH" == "x86_64" ]]; then
      TARGET="x86_64-apple-darwin"
    fi
    ;;
  linux)
    TARGET="x86_64-unknown-linux-gnu"
    if [[ "$ARCH" == "aarch64" ]]; then
      TARGET="aarch64-unknown-linux-gnu"
    fi
    ;;
  windows)
    TARGET="x86_64-pc-windows-msvc"
    ;;
esac

echo -e "Build target: ${YELLOW}${TARGET}${NC}"

# Ensure target is installed
rustup target add "$TARGET"

# Build release
cargo build --release --target "$TARGET"

cd - > /dev/null

# Step 6: Locate and display output
echo ""
echo -e "${BLUE}[✓]${NC} Build complete!"
echo ""
echo -e "${GREEN}Output locations:${NC}"

case "$PLATFORM" in
  macos)
    APP_PATH="frontend/src-tauri/target/$TARGET/release/bundle/macos"
    if [[ -d "$APP_PATH" ]]; then
      echo -e "  macOS App: ${YELLOW}$(ls -d $APP_PATH/*.app 2>/dev/null | head -1)${NC}"
      DMG=$(ls $APP_PATH/*.dmg 2>/dev/null | head -1)
      if [[ -f "$DMG" ]]; then
        echo -e "  DMG Bundle: ${YELLOW}${DMG}${NC}"
      fi
    fi
    ;;
  linux)
    APPIMAGE_PATH="frontend/src-tauri/target/$TARGET/release/bundle/appimage"
    DEB_PATH="frontend/src-tauri/target/$TARGET/release/bundle/deb"
    if [[ -d "$APPIMAGE_PATH" ]]; then
      echo -e "  AppImage: ${YELLOW}$(ls $APPIMAGE_PATH/*.AppImage 2>/dev/null | head -1)${NC}"
    fi
    if [[ -d "$DEB_PATH" ]]; then
      echo -e "  DEB: ${YELLOW}$(ls $DEB_PATH/*.deb 2>/dev/null | head -1)${NC}"
    fi
    ;;
  windows)
    BUNDLE_PATH="frontend/src-tauri/target/$TARGET/release/bundle/msi"
    if [[ -d "$BUNDLE_PATH" ]]; then
      echo -e "  MSI Installer: ${YELLOW}$(ls $BUNDLE_PATH/*.msi 2>/dev/null | head -1)${NC}"
    fi
    BUNDLE_PATH="frontend/src-tauri/target/$TARGET/release/bundle/nsis"
    if [[ -d "$BUNDLE_PATH" ]]; then
      echo -e "  NSIS Installer: ${YELLOW}$(ls $BUNDLE_PATH/*.exe 2>/dev/null | head -1)${NC}"
    fi
    ;;
esac

echo ""
echo -e "${GREEN}Next: Test the app and verify native backend is bundled${NC}"
echo -e "  Run: frontend/src-tauri/target/$TARGET/release/<app-name>"
