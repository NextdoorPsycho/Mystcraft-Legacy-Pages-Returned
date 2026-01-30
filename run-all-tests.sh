#!/usr/bin/env bash

# Mystcraft GameTest Runner
# Runs all GameTests across all platforms and versions
# Outputs results to Test-Results.md

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Output file
OUTPUT_FILE="$SCRIPT_DIR/Test-Results.md"
TEMP_DIR="$SCRIPT_DIR/.test-results-temp"

# Colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Ensure Java 17 on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "$JAVA_HOME")
fi

# Create temp directory
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Configuration
CONFIGS=(
    "fabric:1.20.1:runGametest:Fabric 1.20.1"
    "fabric:1.20.2:runGametest:Fabric 1.20.2"
    "forge:1.20.1:runGameTestServer:Forge 1.20.1"
    "forge:1.20.2:runGameTestServer:Forge 1.20.2"
    "neoforge:1.20.2:runGameTestServer:NeoForge 1.20.2"
)

TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Mystcraft GameTest Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${CYAN}Running tests sequentially (Gradle cache sharing prevents true parallel)${NC}"
echo ""

# Function to run and parse a single test
run_test() {
    local module="$1"
    local version="$2"
    local task="$3"
    local display_name="$4"
    local safe_name="${display_name// /_}"
    local log_file="$TEMP_DIR/${safe_name}.log"
    local results_file="$TEMP_DIR/${safe_name}_results.txt"
    local errors_file="$TEMP_DIR/${safe_name}_errors.txt"
    local counts_file="$TEMP_DIR/${safe_name}_counts.txt"

    echo -e "${YELLOW}[$display_name]${NC} Starting..."

    > "$results_file"
    > "$errors_file"

    local start_time=$(date +%s)

    # Run gradle with timeout
    if command -v timeout &> /dev/null; then
        timeout 300 ./gradlew ":$module:$version:$task" --no-daemon 2>&1 > "$log_file"
        local gradle_exit=$?
    elif command -v gtimeout &> /dev/null; then
        gtimeout 300 ./gradlew ":$module:$version:$task" --no-daemon 2>&1 > "$log_file"
        local gradle_exit=$?
    else
        ./gradlew ":$module:$version:$task" --no-daemon 2>&1 > "$log_file"
        local gradle_exit=$?
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # Parse results
    local passed=0
    local failed=0
    local skipped=0

    while IFS= read -r line; do
        if echo "$line" | grep -qiE "(passed|succeeded|success).*test|test.*passed|test.*succeeded|\[PASS\]|\[SUCCESS\]"; then
            passed=$((passed + 1))
            echo "PASS: $line" >> "$results_file"
        elif echo "$line" | grep -qiE "failed.*test|test.*failed|\[FAIL\]|\[FAILED\]|GameTestServer.*failed"; then
            failed=$((failed + 1))
            echo "FAIL: $line" >> "$results_file"
            echo "$line" >> "$errors_file"
        elif echo "$line" | grep -qiE "skipped|skip"; then
            skipped=$((skipped + 1))
            echo "SKIP: $line" >> "$results_file"
        fi

        if echo "$line" | grep -qiE "exception|error|at [a-z].*\(.*\.java:[0-9]+\)|caused by"; then
            echo "$line" >> "$errors_file"
        fi
    done < "$log_file"

    # Check for summary lines
    local summary=$(grep -iE "[0-9]+ (tests? )?(passed|failed|succeeded)" "$log_file" | tail -1)
    if [[ -n "$summary" ]]; then
        local sum_passed=$(echo "$summary" | grep -oE "[0-9]+ (tests? )?passed" | grep -oE "[0-9]+" | head -1)
        local sum_failed=$(echo "$summary" | grep -oE "[0-9]+ (tests? )?failed" | grep -oE "[0-9]+" | head -1)

        if [[ -n "$sum_passed" ]]; then
            passed=$sum_passed
        fi
        if [[ -n "$sum_failed" ]]; then
            failed=$sum_failed
        fi
    fi

    # Store counts
    echo "$passed $failed $skipped $duration $gradle_exit" > "$counts_file"

    TOTAL_PASSED=$((TOTAL_PASSED + passed))
    TOTAL_FAILED=$((TOTAL_FAILED + failed))
    TOTAL_SKIPPED=$((TOTAL_SKIPPED + skipped))

    # Display result
    if [[ $gradle_exit -ne 0 ]] && [[ $passed -eq 0 ]] && [[ $failed -eq 0 ]]; then
        echo -e "${YELLOW}[$display_name]${NC} ${RED}ERROR${NC} (exit: $gradle_exit, ${duration}s)"
    elif [[ $failed -gt 0 ]]; then
        echo -e "${YELLOW}[$display_name]${NC} ${RED}$passed passed, $failed failed${NC} (${duration}s)"
    elif [[ $passed -gt 0 ]]; then
        echo -e "${YELLOW}[$display_name]${NC} ${GREEN}$passed passed, $failed failed${NC} (${duration}s)"
    else
        echo -e "${YELLOW}[$display_name]${NC} ${YELLOW}No test results detected${NC} (${duration}s)"
    fi
}

# Run all tests sequentially
for config in "${CONFIGS[@]}"; do
    IFS=':' read -r module version task display_name <<< "$config"
    run_test "$module" "$version" "$task" "$display_name"
done

echo ""
echo -e "${GREEN}All tests completed!${NC}"
echo ""

# Generate markdown report
echo -e "${BLUE}Generating Test-Results.md...${NC}"

cat > "$OUTPUT_FILE" << 'HEADER'
# Mystcraft GameTest Results

HEADER

echo "**Generated:** $(date '+%Y-%m-%d %H:%M:%S')" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Summary section
cat >> "$OUTPUT_FILE" << 'SUMMARY_HEADER'
## Summary

| Platform | Passed | Failed | Skipped | Duration | Status |
|----------|--------|--------|---------|----------|--------|
SUMMARY_HEADER

for config in "${CONFIGS[@]}"; do
    IFS=':' read -r module version task display_name <<< "$config"
    safe_name="${display_name// /_}"
    counts_file="$TEMP_DIR/${safe_name}_counts.txt"

    passed=0
    failed=0
    skipped=0
    duration=0
    exit_code=0

    if [[ -f "$counts_file" ]]; then
        read passed failed skipped duration exit_code < "$counts_file"
    fi

    status="N/A"
    if [[ $failed -gt 0 ]]; then
        status="FAIL"
    elif [[ $passed -gt 0 ]]; then
        status="PASS"
    elif [[ $exit_code -ne 0 ]]; then
        status="ERROR"
    fi

    echo "| $display_name | $passed | $failed | $skipped | ${duration}s | $status |" >> "$OUTPUT_FILE"
done

echo "" >> "$OUTPUT_FILE"
echo "**Total: $TOTAL_PASSED passed, $TOTAL_FAILED failed, $TOTAL_SKIPPED skipped**" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Detailed results
echo "---" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "## Detailed Results" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

for config in "${CONFIGS[@]}"; do
    IFS=':' read -r module version task display_name <<< "$config"
    safe_name="${display_name// /_}"
    results_file="$TEMP_DIR/${safe_name}_results.txt"
    errors_file="$TEMP_DIR/${safe_name}_errors.txt"
    log_file="$TEMP_DIR/${safe_name}.log"
    counts_file="$TEMP_DIR/${safe_name}_counts.txt"

    passed=0
    failed=0
    skipped=0
    duration=0
    exit_code=0

    if [[ -f "$counts_file" ]]; then
        read passed failed skipped duration exit_code < "$counts_file"
    fi

    echo "### $display_name" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"

    if [[ $failed -gt 0 ]]; then
        echo "**Status:** FAILED" >> "$OUTPUT_FILE"
    elif [[ $passed -gt 0 ]]; then
        echo "**Status:** PASSED" >> "$OUTPUT_FILE"
    elif [[ $exit_code -ne 0 ]]; then
        echo "**Status:** ERROR (exit code: $exit_code)" >> "$OUTPUT_FILE"
    else
        echo "**Status:** No test results detected" >> "$OUTPUT_FILE"
    fi
    echo "" >> "$OUTPUT_FILE"

    # Passed tests
    if [[ -f "$results_file" ]] && grep -q "^PASS:" "$results_file" 2>/dev/null; then
        echo "#### Passed Tests" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        grep "^PASS:" "$results_file" | sed 's/^PASS: //' >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi

    # Failed tests
    if [[ -f "$results_file" ]] && grep -q "^FAIL:" "$results_file" 2>/dev/null; then
        echo "#### Failed Tests" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        grep "^FAIL:" "$results_file" | sed 's/^FAIL: //' >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi

    # Errors
    if [[ -f "$errors_file" ]] && [[ -s "$errors_file" ]]; then
        echo "#### Errors" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        head -100 "$errors_file" >> "$OUTPUT_FILE"
        if [[ $(wc -l < "$errors_file") -gt 100 ]]; then
            echo "... (truncated, see full log)" >> "$OUTPUT_FILE"
        fi
        echo '```' >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi

    # Test output
    if [[ -f "$log_file" ]]; then
        gametest_output=$(grep -iE "gametest|test.*pass|test.*fail|running test" "$log_file" 2>/dev/null | head -50)
        if [[ -n "$gametest_output" ]]; then
            echo "#### Test Output" >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
            echo '```' >> "$OUTPUT_FILE"
            echo "$gametest_output" >> "$OUTPUT_FILE"
            echo '```' >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
        fi
    fi

    echo "---" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
done

# Configuration reference
cat >> "$OUTPUT_FILE" << 'FOOTER'
## Test Configuration

| Platform | Module | Task |
|----------|--------|------|
| Fabric 1.20.1 | fabric:1.20.1 | runGametest |
| Fabric 1.20.2 | fabric:1.20.2 | runGametest |
| Forge 1.20.1 | forge:1.20.1 | runGameTestServer |
| Forge 1.20.2 | forge:1.20.2 | runGameTestServer |
| NeoForge 1.20.2 | neoforge:1.20.2 | runGameTestServer |

---

*Generated by run-all-tests.sh*
FOOTER

# Cleanup
rm -rf "$TEMP_DIR"

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Test Run Complete${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Results written to: ${GREEN}$OUTPUT_FILE${NC}"
echo ""
echo "Summary:"
if [[ $TOTAL_FAILED -gt 0 ]]; then
    echo -e "  ${GREEN}Passed:${NC}  $TOTAL_PASSED"
    echo -e "  ${RED}Failed:${NC}  $TOTAL_FAILED"
    echo -e "  ${YELLOW}Skipped:${NC} $TOTAL_SKIPPED"
else
    echo -e "  ${GREEN}Passed:${NC}  $TOTAL_PASSED"
    echo -e "  ${GREEN}Failed:${NC}  $TOTAL_FAILED"
    echo -e "  ${YELLOW}Skipped:${NC} $TOTAL_SKIPPED"
fi
