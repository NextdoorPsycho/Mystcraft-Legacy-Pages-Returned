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

# Ensure Java 17
if [[ "$OSTYPE" == "darwin"* ]]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    if [[ -z "$JAVA_HOME" ]]; then
        echo -e "${RED}Error: Java 17 not found.${NC}"
        exit 1
    fi
fi

cd "$SCRIPT_DIR"

echo -e "${CYAN}Running Mystcraft GameTests${NC}"
echo ""

# Run tests for all platforms
./gradlew :fabric:1.20.1:runGametest :fabric:1.20.2:runGametest \
          :forge:1.20.1:runGametest :forge:1.20.2:runGametest \
          :neoforge:1.20.2:runGametest --no-daemon

echo ""
echo -e "${CYAN}Tests complete${NC}"
