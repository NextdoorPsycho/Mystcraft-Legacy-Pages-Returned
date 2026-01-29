# Multi-Version Build System Design

## Overview

A patch-based build system that maintains a single source of truth (main branch) while generating builds for multiple Minecraft versions through versioned patch sets.

## Core Concept

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MAIN BRANCH (1.20.2)                              │
│                         Single Source of Truth                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ apply patches
                                     ▼
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
         ▼                           ▼                           ▼
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   1.21.x Build  │      │   1.20.4 Build  │      │   1.19.4 Build  │
│   patches/1.21  │      │   patches/1.20.4│      │   patches/1.19.4│
└─────────────────┘      └─────────────────┘      └─────────────────┘
         │                           │                           │
         ▼                           ▼                           ▼
    fabric.jar                  fabric.jar                  fabric.jar
    forge.jar                   forge.jar                   forge.jar
    neoforge.jar                neoforge.jar                (no neoforge)
```

## Directory Structure

```
Multiloader/
├── common/                     # Main source (SSOT version)
├── fabric/                     # Fabric adapter
├── forge/                      # Forge adapter
├── neoforge/                   # NeoForge adapter
├── patches/                    # Version-specific patches
│   ├── 1.21/
│   │   ├── manifest.json       # Patch metadata
│   │   ├── gradle.properties   # Version overrides
│   │   ├── common/             # Patches for common module
│   │   │   └── *.patch
│   │   ├── fabric/             # Patches for fabric adapter
│   │   │   └── *.patch
│   │   ├── forge/              # Patches for forge adapter
│   │   │   └── *.patch
│   │   └── neoforge/           # Patches for neoforge adapter
│   │       └── *.patch
│   ├── 1.20.4/
│   │   └── ...
│   └── 1.19.4/
│       └── ...
├── scripts/
│   ├── generate-patches.sh     # Creates patches from manual port
│   ├── apply-patches.sh        # Applies patches for a version
│   ├── build-version.sh        # Builds a specific version
│   ├── build-all-versions.sh   # Builds all versions
│   └── promote-version.sh      # Promotes a version to become new SSOT
└── .github/
    └── workflows/
        └── multi-version-build.yml
```

## Workflow

### Phase 1: Initial Port (Manual, One-Time Per Version)

1. **Start clean** - Ensure git working tree is clean on main branch
2. **Create version branch** - `git checkout -b port/1.21`
3. **Manual port** - Make all necessary changes to support the target version
4. **Verify build** - `./gradlew build` must succeed
5. **Generate patches** - Run `./scripts/generate-patches.sh 1.21`
6. **Store patches** - Patches saved to `patches/1.21/`
7. **Reset** - Return to main branch, discard port changes

### Phase 2: Automated Build (CI/CD)

```
For each version in patches/:
  1. Clone/checkout main branch (clean state)
  2. Apply patches for that version
  3. Build all loaders
  4. Collect artifacts
  5. Reset to clean state
```

---

## Patch Generation Script

### `scripts/generate-patches.sh`

```bash
#!/bin/bash
set -e

VERSION="${1:?Usage: generate-patches.sh <version>}"
PATCH_DIR="patches/${VERSION}"

echo "[generate-patches] Generating patches for MC ${VERSION}"

# Ensure we have changes to capture
if git diff --quiet && git diff --cached --quiet; then
    echo "Error: No changes detected. Port the mod first."
    exit 1
fi

# Create patch directory structure
mkdir -p "${PATCH_DIR}/common"
mkdir -p "${PATCH_DIR}/fabric"
mkdir -p "${PATCH_DIR}/forge"
mkdir -p "${PATCH_DIR}/neoforge"

# Generate unified diff patches per module
# Using git diff with path filters to separate by module

# Common module patches
git diff -- common/ > "${PATCH_DIR}/common/common.patch" 2>/dev/null || true

# Fabric adapter patches
git diff -- fabric/ > "${PATCH_DIR}/fabric/fabric.patch" 2>/dev/null || true

# Forge adapter patches
git diff -- forge/ > "${PATCH_DIR}/forge/forge.patch" 2>/dev/null || true

# NeoForge adapter patches
git diff -- neoforge/ > "${PATCH_DIR}/neoforge/neoforge.patch" 2>/dev/null || true

# Root-level files (gradle.properties, build.gradle, etc.)
git diff -- gradle.properties build.gradle settings.gradle > "${PATCH_DIR}/root.patch" 2>/dev/null || true

# Create manifest with metadata
cat > "${PATCH_DIR}/manifest.json" << EOF
{
  "minecraft_version": "${VERSION}",
  "base_version": "$(git rev-parse HEAD)",
  "generated_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "platforms": {
    "fabric": true,
    "forge": true,
    "neoforge": $([ -s "${PATCH_DIR}/neoforge/neoforge.patch" ] && echo "true" || echo "false")
  }
}
EOF

# Remove empty patch files
find "${PATCH_DIR}" -name "*.patch" -empty -delete

# Report
echo "[generate-patches] Patches generated:"
find "${PATCH_DIR}" -name "*.patch" -exec wc -l {} \;
echo "[generate-patches] Done. Patches saved to ${PATCH_DIR}/"
```

---

## Patch Application Script

### `scripts/apply-patches.sh`

```bash
#!/bin/bash
set -e

VERSION="${1:?Usage: apply-patches.sh <version>}"
PATCH_DIR="patches/${VERSION}"

if [ ! -d "${PATCH_DIR}" ]; then
    echo "Error: No patches found for version ${VERSION}"
    exit 1
fi

echo "[apply-patches] Applying patches for MC ${VERSION}"

# Apply patches in order: root first, then modules
apply_patch() {
    local patch_file="$1"
    if [ -f "${patch_file}" ] && [ -s "${patch_file}" ]; then
        echo "  Applying: ${patch_file}"
        git apply --check "${patch_file}" 2>/dev/null || {
            echo "  Warning: Patch may not apply cleanly, attempting with --3way"
            git apply --3way "${patch_file}" || {
                echo "  Error: Failed to apply ${patch_file}"
                return 1
            }
        }
        git apply "${patch_file}"
    fi
}

# Apply root patches first (gradle.properties, etc.)
apply_patch "${PATCH_DIR}/root.patch"

# Apply module patches
for module in common fabric forge neoforge; do
    for patch in "${PATCH_DIR}/${module}"/*.patch; do
        [ -f "$patch" ] && apply_patch "$patch"
    done
done

echo "[apply-patches] Patches applied successfully for MC ${VERSION}"
```

---

## Build Script

### `scripts/build-version.sh`

```bash
#!/bin/bash
set -e

VERSION="${1:?Usage: build-version.sh <version>}"
OUTPUT_DIR="${2:-build/multiversion}"

echo "[build-version] Building MC ${VERSION}"

# Create output directory
mkdir -p "${OUTPUT_DIR}/${VERSION}"

# Ensure clean state
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Error: Working tree is not clean. Commit or stash changes first."
    exit 1
fi

# Apply patches
./scripts/apply-patches.sh "${VERSION}"

# Build
echo "[build-version] Running gradle build..."
./gradlew clean build --no-daemon

# Collect artifacts
echo "[build-version] Collecting artifacts..."
cp fabric/build/libs/*-${VERSION}*.jar "${OUTPUT_DIR}/${VERSION}/" 2>/dev/null || true
cp forge/build/libs/*-${VERSION}*.jar "${OUTPUT_DIR}/${VERSION}/" 2>/dev/null || true
cp neoforge/build/libs/*-${VERSION}*.jar "${OUTPUT_DIR}/${VERSION}/" 2>/dev/null || true

# Also copy with predictable names
for loader in fabric forge neoforge; do
    jar=$(find ${loader}/build/libs -name "*.jar" ! -name "*-sources*" ! -name "*-dev*" 2>/dev/null | head -1)
    if [ -n "$jar" ]; then
        cp "$jar" "${OUTPUT_DIR}/${VERSION}/mystcraft-${loader}-${VERSION}.jar"
    fi
done

# Reset to clean state
echo "[build-version] Resetting working tree..."
git checkout -- .
git clean -fd

echo "[build-version] Build complete. Artifacts in ${OUTPUT_DIR}/${VERSION}/"
ls -la "${OUTPUT_DIR}/${VERSION}/"
```

---

## Build All Versions Script

### `scripts/build-all-versions.sh`

```bash
#!/bin/bash
set -e

OUTPUT_DIR="${1:-build/multiversion}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[build-all] Starting multi-version build"

# First, build the main version (no patches needed)
echo "[build-all] Building main version (1.20.2)..."
./gradlew clean build --no-daemon
mkdir -p "${OUTPUT_DIR}/1.20.2"
for loader in fabric forge neoforge; do
    jar=$(find ${loader}/build/libs -name "*.jar" ! -name "*-sources*" ! -name "*-dev*" 2>/dev/null | head -1)
    if [ -n "$jar" ]; then
        cp "$jar" "${OUTPUT_DIR}/1.20.2/mystcraft-${loader}-1.20.2.jar"
    fi
done

# Build each patched version
for version_dir in patches/*/; do
    version=$(basename "$version_dir")
    echo "[build-all] Building version ${version}..."
    "${SCRIPT_DIR}/build-version.sh" "$version" "$OUTPUT_DIR"
done

echo "[build-all] All builds complete!"
echo "[build-all] Artifacts:"
find "${OUTPUT_DIR}" -name "*.jar" | sort
```

---

## GitHub Actions Workflow

### `.github/workflows/multi-version-build.yml`

```yaml
name: Multi-Version Build

on:
  push:
    branches: [main, "1.20.2"]
    paths-ignore:
      - "**.md"
      - "docs/**"
  pull_request:
    branches: [main, "1.20.2"]
  workflow_dispatch:
    inputs:
      versions:
        description: "Versions to build (comma-separated, or 'all')"
        required: false
        default: "all"

jobs:
  discover-versions:
    runs-on: ubuntu-latest
    outputs:
      versions: ${{ steps.discover.outputs.versions }}
    steps:
      - uses: actions/checkout@v4

      - id: discover
        name: Discover available versions
        run: |
          # Always include main version
          VERSIONS='["1.20.2"'

          # Add patched versions
          if [ -d "patches" ]; then
            for dir in patches/*/; do
              version=$(basename "$dir")
              VERSIONS="${VERSIONS},\"${version}\""
            done
          fi

          VERSIONS="${VERSIONS}]"
          echo "versions=${VERSIONS}" >> $GITHUB_OUTPUT
          echo "Discovered versions: ${VERSIONS}"

  build:
    needs: discover-versions
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        version: ${{ fromJson(needs.discover-versions.outputs.versions) }}

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Apply patches (if not main version)
        if: matrix.version != '1.20.2'
        run: |
          chmod +x scripts/apply-patches.sh
          ./scripts/apply-patches.sh ${{ matrix.version }}

      - name: Build with Gradle
        run: ./gradlew build --no-daemon

      - name: Collect artifacts
        run: |
          mkdir -p artifacts/${{ matrix.version }}
          for loader in fabric forge neoforge; do
            jar=$(find ${loader}/build/libs -name "*.jar" ! -name "*-sources*" ! -name "*-dev*" 2>/dev/null | head -1)
            if [ -n "$jar" ]; then
              cp "$jar" "artifacts/${{ matrix.version }}/mystcraft-${loader}-${{ matrix.version }}.jar"
            fi
          done

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: mystcraft-${{ matrix.version }}
          path: artifacts/${{ matrix.version }}/*.jar
          retention-days: 30

  collect-all:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Download all artifacts
        uses: actions/download-artifact@v4
        with:
          path: all-versions

      - name: Create combined artifact
        uses: actions/upload-artifact@v4
        with:
          name: mystcraft-all-versions
          path: all-versions/**/*.jar
          retention-days: 30
```

---

## Patch File Format

Patches use standard unified diff format (output of `git diff`). Example:

```diff
diff --git a/common/src/main/java/art/arcane/mystcraft/Mystcraft.java b/common/src/main/java/art/arcane/mystcraft/Mystcraft.java
index abc1234..def5678 100644
--- a/common/src/main/java/art/arcane/mystcraft/Mystcraft.java
+++ b/common/src/main/java/art/arcane/mystcraft/Mystcraft.java
@@ -15,7 +15,7 @@ public class Mystcraft {
     public static final String MOD_ID = "mystcraft";
-    public static final String MC_VERSION = "1.20.2";
+    public static final String MC_VERSION = "1.21";
```

---

## Version-Specific gradle.properties

Each patch set can include a complete `gradle.properties` override:

### `patches/1.21/gradle.properties.override`

```properties
# Minecraft 1.21 Configuration
minecraft_version=1.21
minecraft_version_range=[1.21,1.22)

# Java 21 required for 1.21+
java_version=21
java_version_forge=21
java_version_fabric=21
java_version_neoforge=21

# Updated dependencies
fabric_version=0.100.0+1.21
fabric_loader_version=0.18.4

forge_version=51.0.0
forge_loader_version_range=[51,)

neoforge_version=21.0.0
neoforge_loader_version_range=[1,)
```

---

## Handling Common Porting Changes

### What Typically Changes Between Versions

| Component | Change Frequency | Patch Location |
|-----------|------------------|----------------|
| `gradle.properties` | Always | `root.patch` |
| Mappings/imports | Often | `common/*.patch` |
| Registry methods | Sometimes | `common/*.patch` |
| Rendering APIs | Sometimes | `common/*.patch`, `fabric/*.patch` |
| Networking | Rarely | Adapter patches |
| Loader metadata | Always | Adapter `resources/` |

### Minimizing Patch Size

1. **Abstract version differences** - Create internal helpers in common that can be patched once
2. **Use Architectury API** - Reduces platform-specific code
3. **Avoid hardcoded version strings** - Read from gradle.properties at runtime
4. **Isolate breaking changes** - Keep version-sensitive code in dedicated classes

---

## AI-Assisted Porting

For new version ports, you can use AI (Claude Code, etc.) to:

1. **Analyze changelog** - Identify breaking API changes between versions
2. **Generate initial patches** - Create draft patches based on known patterns
3. **Fix compilation errors** - Iteratively resolve issues
4. **Validate patches** - Ensure patches apply cleanly

### Suggested AI Prompt Template

```
I need to port this Minecraft mod from {SOURCE_VERSION} to {TARGET_VERSION}.

Key changes in {TARGET_VERSION}:
- [List major API changes]

Please analyze these files and generate the necessary changes:
- [List affected files]

Focus on:
1. Import changes
2. Method signature changes
3. Registry API changes
4. Rendering changes (if applicable)
```

---

## Patch Maintenance

### When Main Branch Changes

1. **Minor changes** - Patches likely still apply
2. **Major refactors** - May need to regenerate patches:
   ```bash
   # For each version that needs updating:
   git checkout -b update-port/1.21
   ./scripts/apply-patches.sh 1.21
   # Fix conflicts manually
   # Verify build
   ./scripts/generate-patches.sh 1.21
   git checkout main
   ```

### Patch Conflict Resolution

When patches fail to apply:

1. **Check the failure** - Usually line number drift
2. **Manual resolution** - Edit the patch file or regenerate
3. **Fuzz factor** - `git apply --3way` can help with minor drift

---

## Local Development

### Testing a Patched Version Locally

```bash
# Apply patches without building
./scripts/apply-patches.sh 1.21

# Work with the patched code
./gradlew :fabric:runClient

# Reset when done
git checkout -- .
git clean -fd
```

### Creating a New Version Port

```bash
# 1. Start fresh
git checkout main
git checkout -b port/1.21

# 2. Make changes (manually or with AI assistance)
# ... edit files ...

# 3. Test
./gradlew build

# 4. Generate patches
./scripts/generate-patches.sh 1.21

# 5. Commit patches to main
git checkout main
git add patches/1.21
git commit -m "Add patches for MC 1.21"

# 6. Clean up
git branch -D port/1.21
```

---

## Version Support Matrix

| MC Version | Java | Fabric | Forge | NeoForge | Status |
|------------|------|--------|-------|----------|--------|
| 1.20.2 | 17 | Yes | Yes | Yes | Main branch |
| 1.21.x | 21 | Yes | Yes | Yes | Planned |
| 1.20.4 | 17 | Yes | Yes | Yes | Planned |
| 1.19.4 | 17 | Yes | Yes | No | Planned |

---

## Version Promotion (Updating the SSOT)

When you want to move the project forward to a newer Minecraft version as the main development target, use the **promote-version** workflow. This:

1. Applies the forward patches (old SSOT -> new version)
2. Generates **reverse patches** (new SSOT -> old version)
3. Deletes the now-unnecessary forward patches
4. Leaves you ready to commit the new SSOT

### How Promotion Works

```
BEFORE PROMOTION:
┌─────────────────┐                    ┌─────────────────┐
│   SSOT: 1.20.2  │ ──── patches ────► │   Build: 1.21   │
│   (main branch) │    (forward diff)  │   (patched)     │
└─────────────────┘                    └─────────────────┘

AFTER PROMOTION:
┌─────────────────┐                    ┌─────────────────┐
│   SSOT: 1.21    │ ◄─── patches ───── │   Build: 1.20.2 │
│   (main branch) │    (reverse diff)  │   (patched)     │
└─────────────────┘                    └─────────────────┘
```

The key insight: `git diff A B` is the **inverse** of `git diff B A`. When you promote 1.21 to SSOT, the patches that went 1.20.2->1.21 become 1.21->1.20.2 patches (reversed).

### Promotion Workflow

```bash
# Prerequisites:
# - Clean working tree
# - Patches exist for the target version (patches/1.21/)

# 1. Run the promotion script
./scripts/promote-version.sh 1.21

# 2. Review the changes
git diff
git status

# 3. Verify build works
./gradlew build

# 4. Commit the promotion (you handle this manually per CLAUDE.md)
# git add -A
# git commit -m "Promote SSOT to 1.21, add reverse patches for 1.20.2"
```

### What the Script Does

1. **Applies forward patches** - Transforms code from current SSOT to new version
2. **Generates reverse patches** - Uses `git diff -R` to create patches that go backward
3. **Cleans up** - Removes the old forward patches (now unnecessary)
4. **Reports** - Shows you what was created and next steps

### Post-Promotion State

After promoting 1.21 to SSOT:

| Before | After |
|--------|-------|
| `gradle.properties` has `minecraft_version=1.20.2` | `gradle.properties` has `minecraft_version=1.21` |
| `patches/1.21/` exists (forward patches) | `patches/1.21/` deleted |
| No `patches/1.20.2/` | `patches/1.20.2/` exists (reverse patches) |
| Main branch builds 1.20.2 | Main branch builds 1.21 |

### When to Promote

Promote when:
- The newer version becomes your primary development target
- Most users are on the newer version
- You want new features to land on the newer version first

Don't promote if:
- You're just adding support for a newer version alongside the current one
- The newer version is unstable or in snapshot
- You want to continue primary development on the current version

### Promotion vs. Adding a New Version

| Scenario | Action |
|----------|--------|
| "I want to support 1.21 while staying on 1.20.2" | `generate-patches.sh 1.21` |
| "I want to move development to 1.21" | `promote-version.sh 1.21` |
| "I want to backport to 1.19.4" | `generate-patches.sh 1.19.4` |

---

## Summary

This system allows you to:

1. **Maintain one codebase** - All development happens on the main branch (SSOT)
2. **Support multiple versions** - Via patch sets stored in `patches/`
3. **Automate builds** - GitHub Actions builds all versions on every push
4. **Minimize maintenance** - Only regenerate patches when major changes occur
5. **Leverage AI** - Use AI to assist with porting and patch generation
6. **Move forward gracefully** - Promote newer versions to SSOT with automatic reverse patch generation

The key insight is that ~70% of your code is in `common/` and rarely needs version-specific changes. The adapters (`fabric/`, `forge/`, `neoforge/`) handle loader differences, not version differences. Version differences are handled by patches.

### Scripts Summary

| Script | Purpose |
|--------|---------|
| `generate-patches.sh <version>` | Capture current diff as patches for a version |
| `apply-patches.sh <version>` | Apply patches to transform SSOT -> target version |
| `build-version.sh <version>` | Build a specific version (applies patches, builds, resets) |
| `build-all-versions.sh` | Build SSOT + all patched versions |
| `promote-version.sh <version>` | Make a patched version the new SSOT |
