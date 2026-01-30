#!/usr/bin/env bash

# Mystcraft GameTest Runner
# Runs all GameTests across all platforms and versions IN PARALLEL
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
NC='\033[0m' # No Color

# Ensure Java 17 on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "$JAVA_HOME")
fi

# Create temp directory
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Mystcraft GameTest Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Running all tests in parallel...${NC}"
echo ""

# Function to run a single test configuration
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
    local status_file="$TEMP_DIR/${safe_name}_status.txt"

    echo "Started: $display_name" > "$status_file"

    # Initialize files
    > "$results_file"
    > "$errors_file"

    # Run the gradle task with timeout (5 minutes)
    local start_time=$(date +%s)

    if timeout 300 ./gradlew ":$module:$version:$task" --no-daemon > "$log_file" 2>&1; then
        echo "completed" >> "$status_file"
    else
        local exit_code=$?
        if [[ $exit_code -eq 124 ]]; then
            echo "TIMEOUT after 5 minutes" >> "$log_file"
            echo "timeout" >> "$status_file"
        else
            echo "error:$exit_code" >> "$status_file"
        fi
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    echo "duration:$duration" >> "$status_file"

    # Parse results
    local passed=0
    local failed=0
    local skipped=0

    while IFS= read -r line; do
        # Look for test pass indicators
        if echo "$line" | grep -qiE "(passed|succeeded|success).*test|test.*passed|test.*succeeded|\[PASS\]|\[SUCCESS\]"; then
            passed=$((passed + 1))
            echo "PASS: $line" >> "$results_file"
        # Look for test fail indicators
        elif echo "$line" | grep -qiE "failed.*test|test.*failed|\[FAIL\]|\[FAILED\]|GameTestServer.*failed"; then
            failed=$((failed + 1))
            echo "FAIL: $line" >> "$results_file"
            echo "$line" >> "$errors_file"
        # Look for skipped tests
        elif echo "$line" | grep -qiE "skipped|skip"; then
            skipped=$((skipped + 1))
            echo "SKIP: $line" >> "$results_file"
        fi

        # Capture error stack traces
        if echo "$line" | grep -qiE "exception|error|at [a-z].*\(.*\.java:[0-9]+\)|caused by"; then
            echo "$line" >> "$errors_file"
        fi
    done < "$log_file"

    # Also check for summary lines like "X tests passed, Y tests failed"
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

    # Store counts in file for later retrieval
    echo "$passed $failed $skipped" > "$counts_file"

    echo "Done: $display_name (${duration}s) - $passed passed, $failed failed" >> "$status_file"
}

# Export function and variables for subshells
export -f run_test
export TEMP_DIR
export JAVA_HOME

# Run all tests in parallel
run_test "fabric" "1.20.1" "runGametest" "Fabric 1.20.1" &
PID1=$!
echo "  Started Fabric 1.20.1 (PID: $PID1)"

run_test "fabric" "1.20.2" "runGametest" "Fabric 1.20.2" &
PID2=$!
echo "  Started Fabric 1.20.2 (PID: $PID2)"

run_test "forge" "1.20.1" "runGameTestServer" "Forge 1.20.1" &
PID3=$!
echo "  Started Forge 1.20.1 (PID: $PID3)"

run_test "forge" "1.20.2" "runGameTestServer" "Forge 1.20.2" &
PID4=$!
echo "  Started Forge 1.20.2 (PID: $PID4)"

run_test "neoforge" "1.20.2" "runGameTestServer" "NeoForge 1.20.2" &
PID5=$!
echo "  Started NeoForge 1.20.2 (PID: $PID5)"

echo ""
echo -e "${YELLOW}Waiting for all tests to complete...${NC}"
echo ""

# Wait for all background jobs
wait $PID1 $PID2 $PID3 $PID4 $PID5

echo -e "${GREEN}All tests completed!${NC}"
echo ""

# Calculate totals
TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0

for name in "Fabric_1.20.1" "Fabric_1.20.2" "Forge_1.20.1" "Forge_1.20.2" "NeoForge_1.20.2"; do
    counts_file="$TEMP_DIR/${name}_counts.txt"
    if [[ -f "$counts_file" ]]; then
        read p f s < "$counts_file"
        TOTAL_PASSED=$((TOTAL_PASSED + p))
        TOTAL_FAILED=$((TOTAL_FAILED + f))
        TOTAL_SKIPPED=$((TOTAL_SKIPPED + s))
    fi
done

# Print individual results
echo -e "${BLUE}Individual Results:${NC}"
for name in "Fabric 1.20.1" "Fabric 1.20.2" "Forge 1.20.1" "Forge 1.20.2" "NeoForge 1.20.2"; do
    safe_name="${name// /_}"
    counts_file="$TEMP_DIR/${safe_name}_counts.txt"
    status_file="$TEMP_DIR/${safe_name}_status.txt"

    passed=0
    failed=0
    skipped=0
    duration="?"

    if [[ -f "$counts_file" ]]; then
        read passed failed skipped < "$counts_file"
    fi

    if [[ -f "$status_file" ]]; then
        duration=$(grep "duration:" "$status_file" | cut -d: -f2)
    fi

    if [[ $failed -gt 0 ]]; then
        echo -e "  ${RED}$name:${NC} $passed passed, $failed failed (${duration}s)"
    else
        echo -e "  ${GREEN}$name:${NC} $passed passed, $failed failed (${duration}s)"
    fi
done
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

| Platform | Passed | Failed | Skipped | Status |
|----------|--------|--------|---------|--------|
SUMMARY_HEADER

# Function to add summary row
add_summary_row() {
    local display_name="$1"
    local safe_name="${display_name// /_}"
    local counts_file="$TEMP_DIR/${safe_name}_counts.txt"

    local passed=0
    local failed=0
    local skipped=0

    if [[ -f "$counts_file" ]]; then
        read passed failed skipped < "$counts_file"
    fi

    local status="N/A"
    if [[ $failed -gt 0 ]]; then
        status="FAIL"
    elif [[ $passed -gt 0 ]]; then
        status="PASS"
    fi

    echo "| $display_name | $passed | $failed | $skipped | $status |" >> "$OUTPUT_FILE"
}

add_summary_row "Fabric 1.20.1"
add_summary_row "Fabric 1.20.2"
add_summary_row "Forge 1.20.1"
add_summary_row "Forge 1.20.2"
add_summary_row "NeoForge 1.20.2"

echo "" >> "$OUTPUT_FILE"
echo "**Total: $TOTAL_PASSED passed, $TOTAL_FAILED failed, $TOTAL_SKIPPED skipped**" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Detailed results for each platform
echo "---" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "## Detailed Results" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Function to add detailed results
add_detailed_results() {
    local display_name="$1"
    local safe_name="${display_name// /_}"
    local results_file="$TEMP_DIR/${safe_name}_results.txt"
    local errors_file="$TEMP_DIR/${safe_name}_errors.txt"
    local log_file="$TEMP_DIR/${safe_name}.log"
    local counts_file="$TEMP_DIR/${safe_name}_counts.txt"

    local passed=0
    local failed=0
    local skipped=0

    if [[ -f "$counts_file" ]]; then
        read passed failed skipped < "$counts_file"
    fi

    echo "### $display_name" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"

    if [[ $failed -gt 0 ]]; then
        echo "**Status:** FAILED" >> "$OUTPUT_FILE"
    elif [[ $passed -gt 0 ]]; then
        echo "**Status:** PASSED" >> "$OUTPUT_FILE"
    else
        echo "**Status:** No test results detected" >> "$OUTPUT_FILE"
    fi
    echo "" >> "$OUTPUT_FILE"

    # List passed tests
    if [[ -f "$results_file" ]] && grep -q "^PASS:" "$results_file" 2>/dev/null; then
        echo "#### Passed Tests" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        grep "^PASS:" "$results_file" | sed 's/^PASS: //' >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi

    # List failed tests
    if [[ -f "$results_file" ]] && grep -q "^FAIL:" "$results_file" 2>/dev/null; then
        echo "#### Failed Tests" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        grep "^FAIL:" "$results_file" | sed 's/^FAIL: //' >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi

    # Include errors if any
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

    # Extract relevant log snippets
    if [[ -f "$log_file" ]]; then
        local gametest_output=$(grep -iE "gametest|test.*pass|test.*fail|running test" "$log_file" 2>/dev/null | head -50)
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
}

add_detailed_results "Fabric 1.20.1"
add_detailed_results "Fabric 1.20.2"
add_detailed_results "Forge 1.20.1"
add_detailed_results "Forge 1.20.2"
add_detailed_results "NeoForge 1.20.2"

# Final summary
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
