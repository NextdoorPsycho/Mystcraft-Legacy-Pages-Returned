#!/bin/bash
set -e

NEW_SSOT="${1:?Usage: promote-version.sh <new-ssot-version>}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "$PROJECT_DIR"

# Get current SSOT version
CURRENT_SSOT=$(grep "^minecraft_version=" gradle.properties | cut -d= -f2)

echo "=========================================="
echo "[promote-version] Version Promotion"
echo "=========================================="
echo ""
echo "  Current SSOT: ${CURRENT_SSOT}"
echo "  New SSOT:     ${NEW_SSOT}"
echo ""

if [ "${NEW_SSOT}" = "${CURRENT_SSOT}" ]; then
    echo "Error: ${NEW_SSOT} is already the current SSOT."
    exit 1
fi

# Check if we have patches for the new SSOT
if [ ! -d "patches/${NEW_SSOT}" ]; then
    echo "Error: No patches found for ${NEW_SSOT}."
    echo "You must first port to ${NEW_SSOT} and generate patches."
    echo ""
    echo "Steps:"
    echo "  1. Manually port code to ${NEW_SSOT}"
    echo "  2. ./scripts/generate-patches.sh ${NEW_SSOT}"
    echo "  3. Then run this script again"
    exit 1
fi

# Ensure clean state
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Error: Working tree is not clean."
    echo "Commit, stash, or discard changes first."
    exit 1
fi

echo "[promote-version] Step 1: Apply forward patches (${CURRENT_SSOT} -> ${NEW_SSOT})"
"${SCRIPT_DIR}/apply-patches.sh" "${NEW_SSOT}"

echo ""
echo "[promote-version] Step 2: Generate reverse patches (${NEW_SSOT} -> ${CURRENT_SSOT})"

# The current diff is (CURRENT_SSOT -> NEW_SSOT)
# We need to save the INVERSE as patches for CURRENT_SSOT

REVERSE_PATCH_DIR="${PROJECT_DIR}/patches/${CURRENT_SSOT}"
rm -rf "${REVERSE_PATCH_DIR}"
mkdir -p "${REVERSE_PATCH_DIR}/common"
mkdir -p "${REVERSE_PATCH_DIR}/fabric"
mkdir -p "${REVERSE_PATCH_DIR}/forge"
mkdir -p "${REVERSE_PATCH_DIR}/neoforge"

# Generate reverse patches using git diff -R (reverse)
echo "  Generating reverse patches..."

if ! git diff -R --quiet -- common/; then
    git diff -R -- common/ > "${REVERSE_PATCH_DIR}/common/common.patch"
    echo "    Created: ${CURRENT_SSOT}/common/common.patch"
fi

if ! git diff -R --quiet -- fabric/; then
    git diff -R -- fabric/ > "${REVERSE_PATCH_DIR}/fabric/fabric.patch"
    echo "    Created: ${CURRENT_SSOT}/fabric/fabric.patch"
fi

if ! git diff -R --quiet -- forge/; then
    git diff -R -- forge/ > "${REVERSE_PATCH_DIR}/forge/forge.patch"
    echo "    Created: ${CURRENT_SSOT}/forge/forge.patch"
fi

if ! git diff -R --quiet -- neoforge/; then
    git diff -R -- neoforge/ > "${REVERSE_PATCH_DIR}/neoforge/neoforge.patch"
    echo "    Created: ${CURRENT_SSOT}/neoforge/neoforge.patch"
fi

if ! git diff -R --quiet -- gradle.properties build.gradle settings.gradle; then
    git diff -R -- gradle.properties build.gradle settings.gradle > "${REVERSE_PATCH_DIR}/root.patch"
    echo "    Created: ${CURRENT_SSOT}/root.patch"
fi

# Create manifest for the old version
cat > "${REVERSE_PATCH_DIR}/manifest.json" << EOF
{
  "minecraft_version": "${CURRENT_SSOT}",
  "base_version": "promoted-from-ssot",
  "generated_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "promoted_to": "${NEW_SSOT}",
  "platforms": {
    "fabric": true,
    "forge": true,
    "neoforge": true
  },
  "notes": "Auto-generated reverse patches when ${NEW_SSOT} became SSOT"
}
EOF

# Clean up empty files/dirs
find "${REVERSE_PATCH_DIR}" -name "*.patch" -empty -delete
find "${REVERSE_PATCH_DIR}" -type d -empty -delete 2>/dev/null || true

echo ""
echo "[promote-version] Step 3: Remove old forward patches for ${NEW_SSOT}"
rm -rf "patches/${NEW_SSOT}"
echo "  Deleted: patches/${NEW_SSOT}/"

echo ""
echo "[promote-version] Step 4: Summary of new patch structure"
echo ""
echo "  patches/"
for dir in patches/*/; do
    if [ -d "$dir" ]; then
        version=$(basename "$dir")
        patch_count=$(find "$dir" -name "*.patch" 2>/dev/null | wc -l | tr -d ' ')
        echo "    ${version}/ (${patch_count} patches)"
    fi
done

echo ""
echo "=========================================="
echo "[promote-version] IMPORTANT: Manual Steps Required"
echo "=========================================="
echo ""
echo "The working tree now contains the ${NEW_SSOT} code."
echo "The patches for ${CURRENT_SSOT} have been generated."
echo ""
echo "To complete the promotion, you have two options:"
echo ""
echo "Option A: Commit directly (if you're sure)"
echo "  git add -A"
echo "  git commit -m 'Promote SSOT to ${NEW_SSOT}, add reverse patches for ${CURRENT_SSOT}'"
echo ""
echo "Option B: Review first"
echo "  git diff                    # Review the changes"
echo "  ./gradlew build             # Verify it builds"
echo "  # Then commit when satisfied"
echo ""
echo "To abort and restore original state:"
echo "  git checkout -- ."
echo "  git clean -fd"
echo "  rm -rf patches/${CURRENT_SSOT}"
echo ""
