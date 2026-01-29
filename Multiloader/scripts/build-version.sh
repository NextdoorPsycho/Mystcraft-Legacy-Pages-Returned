#!/bin/bash
set -e

VERSION="${1:?Usage: build-version.sh <version> [output-dir]}"
OUTPUT_DIR="${2:-build/multiversion}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "$PROJECT_DIR"

echo "[build-version] Building MC ${VERSION}"

# Create output directory
mkdir -p "${OUTPUT_DIR}/${VERSION}"

# Check if this is a patched version or the main version
MAIN_VERSION=$(grep "^minecraft_version=" gradle.properties | cut -d= -f2)

if [ "${VERSION}" = "${MAIN_VERSION}" ]; then
    echo "[build-version] Building main version (no patches needed)"
else
    # Ensure clean state before applying patches
    if ! git diff --quiet || ! git diff --cached --quiet; then
        echo "Error: Working tree is not clean."
        echo "Commit, stash, or discard changes before building patched versions."
        exit 1
    fi

    # Apply patches
    "${SCRIPT_DIR}/apply-patches.sh" "${VERSION}"
fi

# Build
echo "[build-version] Running gradle build..."
./gradlew clean build --no-daemon --stacktrace

# Collect artifacts
echo "[build-version] Collecting artifacts..."
for loader in fabric forge neoforge; do
    # Find the main jar (not sources, not dev)
    jar=$(find "${loader}/build/libs" -name "*.jar" \
        ! -name "*-sources*" \
        ! -name "*-dev*" \
        ! -name "*-shadow*" \
        2>/dev/null | head -1)

    if [ -n "$jar" ] && [ -f "$jar" ]; then
        # Copy with standardized name
        cp "$jar" "${OUTPUT_DIR}/${VERSION}/mystcraft-${loader}-${VERSION}.jar"
        echo "  Collected: mystcraft-${loader}-${VERSION}.jar"
    fi
done

# Reset if we applied patches
if [ "${VERSION}" != "${MAIN_VERSION}" ]; then
    echo "[build-version] Resetting working tree..."
    git checkout -- .
    git clean -fd -e "build/" -e "${OUTPUT_DIR}/"
fi

echo ""
echo "[build-version] Build complete for MC ${VERSION}"
echo "[build-version] Artifacts:"
ls -la "${OUTPUT_DIR}/${VERSION}/"
