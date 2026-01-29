#!/bin/bash
set -e

VERSION="${1:?Usage: generate-patches.sh <version>}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${PROJECT_DIR}/patches/${VERSION}"

cd "$PROJECT_DIR"

echo "[generate-patches] Generating patches for MC ${VERSION}"

# Ensure we have changes to capture
if git diff --quiet && git diff --cached --quiet; then
    echo "Error: No changes detected. Port the mod first."
    exit 1
fi

# Create patch directory structure
rm -rf "${PATCH_DIR}"
mkdir -p "${PATCH_DIR}/common"
mkdir -p "${PATCH_DIR}/fabric"
mkdir -p "${PATCH_DIR}/forge"
mkdir -p "${PATCH_DIR}/neoforge"

# Generate unified diff patches per module
echo "[generate-patches] Capturing diffs..."

# Common module patches
if ! git diff --quiet -- common/; then
    git diff -- common/ > "${PATCH_DIR}/common/common.patch"
    echo "  Created: common/common.patch"
fi

# Fabric adapter patches
if ! git diff --quiet -- fabric/; then
    git diff -- fabric/ > "${PATCH_DIR}/fabric/fabric.patch"
    echo "  Created: fabric/fabric.patch"
fi

# Forge adapter patches
if ! git diff --quiet -- forge/; then
    git diff -- forge/ > "${PATCH_DIR}/forge/forge.patch"
    echo "  Created: forge/forge.patch"
fi

# NeoForge adapter patches
if ! git diff --quiet -- neoforge/; then
    git diff -- neoforge/ > "${PATCH_DIR}/neoforge/neoforge.patch"
    echo "  Created: neoforge/neoforge.patch"
fi

# Root-level files (gradle.properties, build.gradle, settings.gradle)
if ! git diff --quiet -- gradle.properties build.gradle settings.gradle; then
    git diff -- gradle.properties build.gradle settings.gradle > "${PATCH_DIR}/root.patch"
    echo "  Created: root.patch"
fi

# Create manifest with metadata
BASE_COMMIT=$(git rev-parse HEAD)
GENERATED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Determine which platforms have patches
HAS_FABRIC="false"
HAS_FORGE="false"
HAS_NEOFORGE="false"
[ -s "${PATCH_DIR}/fabric/fabric.patch" ] && HAS_FABRIC="true"
[ -s "${PATCH_DIR}/forge/forge.patch" ] && HAS_FORGE="true"
[ -s "${PATCH_DIR}/neoforge/neoforge.patch" ] && HAS_NEOFORGE="true"

cat > "${PATCH_DIR}/manifest.json" << EOF
{
  "minecraft_version": "${VERSION}",
  "base_version": "${BASE_COMMIT}",
  "generated_at": "${GENERATED_AT}",
  "platforms": {
    "fabric": ${HAS_FABRIC},
    "forge": ${HAS_FORGE},
    "neoforge": ${HAS_NEOFORGE}
  },
  "notes": ""
}
EOF

# Remove empty patch files
find "${PATCH_DIR}" -name "*.patch" -empty -delete

# Remove empty directories
find "${PATCH_DIR}" -type d -empty -delete 2>/dev/null || true

# Report
echo ""
echo "[generate-patches] Patch summary:"
TOTAL_LINES=0
while IFS= read -r patch; do
    lines=$(wc -l < "$patch" | tr -d ' ')
    TOTAL_LINES=$((TOTAL_LINES + lines))
    echo "  $(basename "$(dirname "$patch")")/$(basename "$patch"): ${lines} lines"
done < <(find "${PATCH_DIR}" -name "*.patch" 2>/dev/null)

echo ""
echo "[generate-patches] Total: ${TOTAL_LINES} lines of patches"
echo "[generate-patches] Done. Patches saved to patches/${VERSION}/"
