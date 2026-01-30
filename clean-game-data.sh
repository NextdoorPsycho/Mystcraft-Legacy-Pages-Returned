#!/bin/bash
# Clean Game/Server Data Script
# Removes all game instance files: worlds, configs, logs, crash reports, etc.
# This script clears all run directories while preserving the directory structure.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Mystcraft Game Data Cleanup Script ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Directories to clean within each run instance (client/server/gametest)
CLEAN_DIRS=(
    "saves"
    "world"
    "logs"
    "crash-reports"
    "config"
    "defaultconfigs"
    "mods"
    "resourcepacks"
    "data"
    "resources"
    "screenshots"
    "schematics"
    "shaderpacks"
)

# Files to remove
CLEAN_FILES=(
    "options.txt"
    "usercache.json"
    "usernamecache.json"
    "command_history.txt"
    "server.properties"
    "banned-ips.json"
    "banned-players.json"
    "ops.json"
    "whitelist.json"
    "eula.txt"
    ".DS_Store"
)

# Find all runs directories
RUNS_DIRS=$(find . -type d \( -name "runs" -o -name "run" -o -name "run-data" \) 2>/dev/null | grep -v ".gradle" | grep -v "net/neoforged" || true)

if [ -z "$RUNS_DIRS" ]; then
    echo -e "${YELLOW}No run directories found.${NC}"
    exit 0
fi

echo "Found run directories:"
echo "$RUNS_DIRS" | while read -r dir; do
    echo "  - $dir"
done
echo ""

# Ask for confirmation
read -p "This will DELETE all game data (worlds, configs, logs, etc.). Continue? [y/N] " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Aborted.${NC}"
    exit 0
fi

echo ""
echo "Cleaning game data..."

TOTAL_CLEANED=0

# Process each runs directory
echo "$RUNS_DIRS" | while read -r runs_dir; do
    if [ -d "$runs_dir" ]; then
        echo -e "${GREEN}Processing: $runs_dir${NC}"

        # Find all subdirectories (client, server, gametest, etc.)
        for instance_dir in "$runs_dir"/*/; do
            if [ -d "$instance_dir" ]; then
                instance_name=$(basename "$instance_dir")
                echo "  Cleaning: $instance_name"

                # Remove directories
                for clean_dir in "${CLEAN_DIRS[@]}"; do
                    target="$instance_dir$clean_dir"
                    if [ -d "$target" ]; then
                        rm -rf "$target"
                        echo "    Removed: $clean_dir/"
                        ((TOTAL_CLEANED++)) || true
                    fi
                done

                # Remove files
                for clean_file in "${CLEAN_FILES[@]}"; do
                    target="$instance_dir$clean_file"
                    if [ -f "$target" ]; then
                        rm -f "$target"
                        echo "    Removed: $clean_file"
                        ((TOTAL_CLEANED++)) || true
                    fi
                done
            fi
        done
    fi
done

echo ""
echo -e "${GREEN}=== Cleanup Complete ===${NC}"
echo "Run directories have been cleared."
echo ""
echo "Note: Empty directories may remain. The next game launch will recreate needed files."
