#!/bin/bash
set -e

VERSION="${1:?Usage: apply-patches.sh <version>}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${PROJECT_DIR}/patches/${VERSION}"

cd "$PROJECT_DIR"

if [ ! -d "${PATCH_DIR}" ]; then
    echo "Error: No patches found for version ${VERSION}"
    echo "Available versions:"
    ls -1 patches/ 2>/dev/null || echo "  (none)"
    exit 1
fi

echo "[apply-patches] Applying patches for MC ${VERSION}"

# Function to apply a single patch
apply_patch() {
    local patch_file="$1"
    local relative_path="${patch_file#${PROJECT_DIR}/}"

    if [ ! -f "${patch_file}" ] || [ ! -s "${patch_file}" ]; then
        return 0
    fi

    echo "  Applying: ${relative_path}"

    # First, check if patch applies cleanly
    if git apply --check "${patch_file}" 2>/dev/null; then
        git apply "${patch_file}"
    else
        # Try with 3-way merge for fuzzy application
        echo "    Warning: Patch doesn't apply cleanly, attempting --3way..."
        if git apply --3way "${patch_file}" 2>/dev/null; then
            echo "    Applied with 3-way merge"
        else
            echo "    Error: Failed to apply ${relative_path}"
            echo "    You may need to regenerate patches for this version."
            return 1
        fi
    fi
}

# Track success
FAILED=0

# Apply root patches first (gradle.properties, etc.)
if [ -f "${PATCH_DIR}/root.patch" ]; then
    apply_patch "${PATCH_DIR}/root.patch" || FAILED=1
fi

# Apply module patches in order: common first, then adapters
for module in common fabric forge neoforge; do
    if [ -d "${PATCH_DIR}/${module}" ]; then
        for patch in "${PATCH_DIR}/${module}"/*.patch; do
            [ -f "$patch" ] && { apply_patch "$patch" || FAILED=1; }
        done
    fi
done

if [ $FAILED -eq 1 ]; then
    echo ""
    echo "[apply-patches] Warning: Some patches failed to apply"
    echo "[apply-patches] You may need to resolve conflicts manually"
    exit 1
fi

echo ""
echo "[apply-patches] All patches applied successfully for MC ${VERSION}"
