#!/bin/bash

# Mystcraft Test Runner - 1.20.x only

set -o pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Ensure Java 17 (Fabric/Forge) and Java 21 (NeoForge 1.20.6) when available
if [[ "$OSTYPE" == "darwin"* ]]; then
    JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    JAVA_21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
    if [[ -z "$JAVA_17_HOME" ]]; then
        echo -e "${RED}Error: Java 17 not found.${NC}"
        exit 1
    fi
fi

cd "$SCRIPT_DIR"

echo -e "${CYAN}Running Mystcraft GameTests${NC}"
echo ""

# Use the standard Gradle cache location (should already have the wrapper distribution)
export GRADLE_USER_HOME="${HOME}/.gradle"

# Use Java 21 for Gradle itself when available (Fabric Loom requires 21+)
if [[ "$OSTYPE" == "darwin"* && -n "$JAVA_21_HOME" ]]; then
    GRADLE_JAVA_HOME="$JAVA_21_HOME"
else
    GRADLE_JAVA_HOME="$JAVA_17_HOME"
fi

# Run tests for Fabric/Forge (Java 17)
if [[ "$OSTYPE" == "darwin"* ]]; then
    JAVA_HOME="$GRADLE_JAVA_HOME" ./gradlew :fabric:1.20.1:runGametest :fabric:1.20.2:runGametest \
              :fabric:1.20.4:runGametest \
              :forge:1.20.1:runGametest :forge:1.20.2:runGametest :forge:1.20.4:runGametest :forge:1.20.6:runGametest --no-daemon
else
    ./gradlew :fabric:1.20.1:runGametest :fabric:1.20.2:runGametest \
              :fabric:1.20.4:runGametest \
              :forge:1.20.1:runGametest :forge:1.20.2:runGametest :forge:1.20.4:runGametest :forge:1.20.6:runGametest --no-daemon
fi

# Run tests for NeoForge (Java 21)
if [[ "$OSTYPE" == "darwin"* ]]; then
    if [[ -n "$JAVA_21_HOME" ]]; then
        JAVA_HOME="$JAVA_21_HOME" ./gradlew :neoforge:1.20.4:runGameTestServer :neoforge:1.20.6:runGameTestServer --no-daemon
    else
        echo -e "${YELLOW}Warning: Java 21 not found; skipping NeoForge GameTests.${NC}"
    fi
else
    ./gradlew :neoforge:1.20.4:runGameTestServer :neoforge:1.20.6:runGameTestServer --no-daemon
fi

echo ""
echo -e "${CYAN}Tests complete${NC}"
