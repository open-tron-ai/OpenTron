#!/bin/bash

# Create GitHub Release Script
# Usage: ./scripts/create-release.sh v1.0.0

set -e

VERSION=${1:-v1.0.0}

# Verify tag format
if ! [[ $VERSION =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "❌ Invalid version format. Use: v1.0.0"
  exit 1
fi

echo "📦 Creating release: $VERSION"
echo ""

# Check if tag exists
if git rev-parse "$VERSION" >/dev/null 2>&1; then
  echo "✓ Tag $VERSION already exists"
else
  echo "Creating tag $VERSION..."
  git tag -a "$VERSION" -m "Release $VERSION"
  echo "✓ Tag created"
fi

# Push tag to GitHub
echo "Pushing tag to GitHub..."
git push origin "$VERSION"

echo ""
echo "✅ Release triggered!"
echo ""
echo "GitHub Actions will:"
echo "  1. Create GitHub Release"
echo "  2. Build 5 native backends (Linux x86/ARM, macOS x86/ARM, Windows)"
echo "  3. Build 5 desktop apps (macOS DMG, Linux AppImage/DEB, Windows EXE/MSI)"
echo "  4. Upload all assets to release"
echo ""
echo "📍 Monitor at: https://github.com/$GITHUB_REPOSITORY/actions"
echo "📍 Release at: https://github.com/$GITHUB_REPOSITORY/releases/tag/$VERSION"
echo ""
echo "Expected timeline:"
echo "  • Native builds: 30 min (parallel)"
echo "  • Desktop builds: 20 min (parallel)"
echo "  • Total: ~40 minutes"
