#!/bin/bash
set -e

OUTPUT_DIR="${1:-build/multiversion}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "$PROJECT_DIR"

echo "=========================================="
echo "[build-all] Multi-Version Build System"
echo "=========================================="
echo ""

# Ensure clean state
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Error: Working tree is not clean."
    echo "Commit, stash, or discard changes before running multi-version build."
    exit 1
fi

# Get main version from gradle.properties
MAIN_VERSION=$(grep "^minecraft_version=" gradle.properties | cut -d= -f2)
echo "[build-all] Main version: ${MAIN_VERSION}"

# Discover all versions to build
VERSIONS=("${MAIN_VERSION}")
if [ -d "patches" ]; then
    for version_dir in patches/*/; do
        if [ -d "$version_dir" ]; then
            version=$(basename "$version_dir")
            VERSIONS+=("$version")
        fi
    done
fi

echo "[build-all] Versions to build: ${VERSIONS[*]}"
echo ""

# Create output directory
mkdir -p "${OUTPUT_DIR}"

# Track results
declare -A BUILD_RESULTS

# Build each version
for version in "${VERSIONS[@]}"; do
    echo "=========================================="
    echo "[build-all] Building: ${version}"
    echo "=========================================="

    if "${SCRIPT_DIR}/build-version.sh" "$version" "$OUTPUT_DIR"; then
        BUILD_RESULTS["$version"]="SUCCESS"
    else
        BUILD_RESULTS["$version"]="FAILED"
        echo "[build-all] Warning: Build failed for ${version}"
    fi

    echo ""
done

# Summary
echo "=========================================="
echo "[build-all] Build Summary"
echo "=========================================="

for version in "${VERSIONS[@]}"; do
    status="${BUILD_RESULTS[$version]}"
    if [ "$status" = "SUCCESS" ]; then
        echo "  [OK] ${version}"
    else
        echo "  [FAILED] ${version}"
    fi
done

echo ""
echo "[build-all] All artifacts:"
find "${OUTPUT_DIR}" -name "*.jar" | sort

# Count successes/failures
success_count=0
fail_count=0
for version in "${VERSIONS[@]}"; do
    if [ "${BUILD_RESULTS[$version]}" = "SUCCESS" ]; then
        ((success_count++))
    else
        ((fail_count++))
    fi
done

echo ""
echo "[build-all] Complete: ${success_count} succeeded, ${fail_count} failed"

# Exit with error if any failed
[ $fail_count -eq 0 ] || exit 1
