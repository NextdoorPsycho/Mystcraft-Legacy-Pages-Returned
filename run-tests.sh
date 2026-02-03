#!/bin/bash

# Mystcraft Test Runner
# Runs all GameTests across loaders and provides detailed logging with summary

set -o pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/test-logs"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$LOG_DIR/test_run_$TIMESTAMP.log"
SUMMARY_FILE="$LOG_DIR/test_summary_$TIMESTAMP.txt"

# Counters
declare -A TEST_RESULTS
TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0
declare -a FAILED_TESTS
declare -a PASSED_TESTS
declare -a ERROR_MESSAGES

# Ensure Java 17
if [[ "$OSTYPE" == "darwin"* ]]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    if [[ -z "$JAVA_HOME" ]]; then
        echo -e "${RED}Error: Java 17 not found. Please install Java 17.${NC}"
        exit 1
    fi
fi

# Create log directory
mkdir -p "$LOG_DIR"

# Header
print_header() {
    echo -e "${CYAN}"
    echo "============================================================"
    echo "              MYSTCRAFT TEST RUNNER"
    echo "============================================================"
    echo -e "${NC}"
    echo -e "${WHITE}Timestamp:${NC} $(date)"
    echo -e "${WHITE}Java Home:${NC} $JAVA_HOME"
    echo -e "${WHITE}Log File:${NC} $LOG_FILE"
    echo ""
}

# Log function - writes to both console and file
log() {
    echo -e "$1" | tee -a "$LOG_FILE"
}

log_raw() {
    echo "$1" >> "$LOG_FILE"
}

# Parse test output for results
parse_test_line() {
    local line="$1"

    # GameTest patterns
    if [[ "$line" =~ "GameTest".*"passed" ]] || [[ "$line" =~ "Test passed:".*  ]] || [[ "$line" =~ \[PASS\] ]]; then
        local test_name=$(echo "$line" | grep -oE '[a-zA-Z_][a-zA-Z0-9_]*' | tail -1)
        if [[ -n "$test_name" ]]; then
            PASSED_TESTS+=("$test_name")
            ((TOTAL_PASSED++))
            log "${GREEN}[PASS]${NC} $test_name"
        fi
    elif [[ "$line" =~ "GameTest".*"failed" ]] || [[ "$line" =~ "Test failed:".*  ]] || [[ "$line" =~ \[FAIL\] ]]; then
        local test_name=$(echo "$line" | grep -oE '[a-zA-Z_][a-zA-Z0-9_]*' | tail -1)
        if [[ -n "$test_name" ]]; then
            FAILED_TESTS+=("$test_name")
            ((TOTAL_FAILED++))
            log "${RED}[FAIL]${NC} $test_name"
        fi
    elif [[ "$line" =~ "Exception" ]] || [[ "$line" =~ "Error:" ]] || [[ "$line" =~ "error:" ]]; then
        ERROR_MESSAGES+=("$line")
        log "${RED}[ERROR]${NC} $line"
    elif [[ "$line" =~ "SKIPPED" ]] || [[ "$line" =~ "skipped" ]]; then
        ((TOTAL_SKIPPED++))
    fi
}

# Run tests for a specific module
run_module_tests() {
    local module="$1"
    local display_name="$2"

    log ""
    log "${BLUE}------------------------------------------------------------${NC}"
    log "${WHITE}Running: $display_name${NC}"
    log "${BLUE}------------------------------------------------------------${NC}"

    local start_time=$(date +%s)
    local module_passed=0
    local module_failed=0
    local exit_code=0

    # Run gradle task and capture output
    while IFS= read -r line; do
        log_raw "$line"

        # Check for test results in output
        if [[ "$line" =~ "passed" ]] && [[ "$line" =~ "test" ]]; then
            parse_test_line "$line"
        elif [[ "$line" =~ "failed" ]] && [[ "$line" =~ "test" ]]; then
            parse_test_line "$line"
        elif [[ "$line" =~ "GameTest" ]]; then
            parse_test_line "$line"
        elif [[ "$line" =~ "Exception" ]] || [[ "$line" =~ "Error" ]]; then
            parse_test_line "$line"
        fi

        # Also print important lines to console
        if [[ "$line" =~ "BUILD" ]] || [[ "$line" =~ "Test" ]] || [[ "$line" =~ "GameTest" ]] || [[ "$line" =~ "FAILED" ]] || [[ "$line" =~ "SUCCESS" ]]; then
            echo "$line"
        fi
    done < <(./gradlew "$module" --console=plain 2>&1; echo "EXIT_CODE:$?")

    # Extract exit code from output
    if [[ "${line}" =~ EXIT_CODE:([0-9]+) ]]; then
        exit_code="${BASH_REMATCH[1]}"
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log ""
    log "${WHITE}Duration:${NC} ${duration}s"

    if [[ $exit_code -eq 0 ]]; then
        log "${GREEN}$display_name: BUILD SUCCESSFUL${NC}"
        TEST_RESULTS["$display_name"]="PASS"
    else
        log "${RED}$display_name: BUILD FAILED${NC}"
        TEST_RESULTS["$display_name"]="FAIL"
    fi

    return $exit_code
}

# Print summary chart
print_summary() {
    local total=$((TOTAL_PASSED + TOTAL_FAILED))
    local pass_percent=0
    local fail_percent=0

    if [[ $total -gt 0 ]]; then
        pass_percent=$((TOTAL_PASSED * 100 / total))
        fail_percent=$((TOTAL_FAILED * 100 / total))
    fi

    echo "" | tee -a "$LOG_FILE"
    echo -e "${CYAN}============================================================${NC}" | tee -a "$LOG_FILE"
    echo -e "${WHITE}                    TEST SUMMARY${NC}" | tee -a "$LOG_FILE"
    echo -e "${CYAN}============================================================${NC}" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"

    # Module Results Table
    echo -e "${WHITE}Module Results:${NC}" | tee -a "$LOG_FILE"
    echo -e "${BLUE}+----------------------------------+----------+${NC}" | tee -a "$LOG_FILE"
    printf "${BLUE}|${NC} %-32s ${BLUE}|${NC} %-8s ${BLUE}|${NC}\n" "Module" "Status" | tee -a "$LOG_FILE"
    echo -e "${BLUE}+----------------------------------+----------+${NC}" | tee -a "$LOG_FILE"

    for module in "${!TEST_RESULTS[@]}"; do
        local status="${TEST_RESULTS[$module]}"
        local color=$GREEN
        if [[ "$status" == "FAIL" ]]; then
            color=$RED
        fi
        printf "${BLUE}|${NC} %-32s ${BLUE}|${NC} ${color}%-8s${NC} ${BLUE}|${NC}\n" "$module" "$status" | tee -a "$LOG_FILE"
    done
    echo -e "${BLUE}+----------------------------------+----------+${NC}" | tee -a "$LOG_FILE"

    echo "" | tee -a "$LOG_FILE"

    # Statistics
    echo -e "${WHITE}Statistics:${NC}" | tee -a "$LOG_FILE"
    echo -e "  ${GREEN}Passed:${NC}  $TOTAL_PASSED" | tee -a "$LOG_FILE"
    echo -e "  ${RED}Failed:${NC}  $TOTAL_FAILED" | tee -a "$LOG_FILE"
    echo -e "  ${YELLOW}Skipped:${NC} $TOTAL_SKIPPED" | tee -a "$LOG_FILE"
    echo -e "  ${WHITE}Total:${NC}   $total" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"

    # Visual Bar Chart
    echo -e "${WHITE}Pass/Fail Chart:${NC}" | tee -a "$LOG_FILE"
    local bar_width=50
    local pass_bars=$((pass_percent * bar_width / 100))
    local fail_bars=$((fail_percent * bar_width / 100))

    # Ensure at least 1 bar if there are any
    if [[ $TOTAL_PASSED -gt 0 ]] && [[ $pass_bars -eq 0 ]]; then
        pass_bars=1
    fi
    if [[ $TOTAL_FAILED -gt 0 ]] && [[ $fail_bars -eq 0 ]]; then
        fail_bars=1
    fi

    printf "  Pass [" | tee -a "$LOG_FILE"
    for ((i=0; i<pass_bars; i++)); do printf "${GREEN}#${NC}"; done | tee -a "$LOG_FILE"
    for ((i=pass_bars; i<bar_width; i++)); do printf " "; done | tee -a "$LOG_FILE"
    printf "] %3d%%\n" "$pass_percent" | tee -a "$LOG_FILE"

    printf "  Fail [" | tee -a "$LOG_FILE"
    for ((i=0; i<fail_bars; i++)); do printf "${RED}#${NC}"; done | tee -a "$LOG_FILE"
    for ((i=fail_bars; i<bar_width; i++)); do printf " "; done | tee -a "$LOG_FILE"
    printf "] %3d%%\n" "$fail_percent" | tee -a "$LOG_FILE"

    echo "" | tee -a "$LOG_FILE"

    # Failed Tests List
    if [[ ${#FAILED_TESTS[@]} -gt 0 ]]; then
        echo -e "${RED}Failed Tests:${NC}" | tee -a "$LOG_FILE"
        for test in "${FAILED_TESTS[@]}"; do
            echo -e "  - $test" | tee -a "$LOG_FILE"
        done
        echo "" | tee -a "$LOG_FILE"
    fi

    # Errors Summary
    if [[ ${#ERROR_MESSAGES[@]} -gt 0 ]]; then
        echo -e "${RED}Errors Encountered:${NC}" | tee -a "$LOG_FILE"
        local error_count=0
        for error in "${ERROR_MESSAGES[@]}"; do
            ((error_count++))
            if [[ $error_count -le 10 ]]; then
                echo -e "  ${RED}*${NC} $error" | tee -a "$LOG_FILE"
            fi
        done
        if [[ $error_count -gt 10 ]]; then
            echo -e "  ... and $((error_count - 10)) more errors (see full log)" | tee -a "$LOG_FILE"
        fi
        echo "" | tee -a "$LOG_FILE"
    fi

    # Final Status
    echo -e "${CYAN}============================================================${NC}" | tee -a "$LOG_FILE"
    if [[ $TOTAL_FAILED -eq 0 ]] && [[ ${#TEST_RESULTS[@]} -gt 0 ]]; then
        echo -e "${GREEN}                    ALL TESTS PASSED${NC}" | tee -a "$LOG_FILE"
    elif [[ $TOTAL_FAILED -gt 0 ]]; then
        echo -e "${RED}                    SOME TESTS FAILED${NC}" | tee -a "$LOG_FILE"
    else
        echo -e "${YELLOW}                    NO TESTS EXECUTED${NC}" | tee -a "$LOG_FILE"
    fi
    echo -e "${CYAN}============================================================${NC}" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"
    echo -e "${WHITE}Full log:${NC} $LOG_FILE" | tee -a "$LOG_FILE"
}

# Main execution
main() {
    cd "$SCRIPT_DIR"

    print_header | tee "$LOG_FILE"

    local overall_exit=0

    # Check what test tasks are available
    log "${YELLOW}Detecting available test configurations...${NC}"
    log ""

    # Run game tests for each loader/version
    run_module_tests ":fabric:1.18.2:runGametest" "Fabric 1.18.2" || overall_exit=1
    run_module_tests ":fabric:1.19.2:runGametest" "Fabric 1.19.2" || overall_exit=1
    run_module_tests ":fabric:1.20.1:runGametest" "Fabric 1.20.1" || overall_exit=1
    run_module_tests ":fabric:1.20.2:runGametest" "Fabric 1.20.2" || overall_exit=1
    # Forge and NeoForge may have different task names - uncomment if available:
    # run_module_tests ":forge:1.20.1:runGametest" "Forge 1.20.1" || overall_exit=1
    # run_module_tests ":forge:1.20.2:runGametest" "Forge 1.20.2" || overall_exit=1
    # run_module_tests ":neoforge:1.20.2:runGametest" "NeoForge 1.20.2" || overall_exit=1

    print_summary

    exit $overall_exit
}

# Handle arguments
case "${1:-}" in
    -h|--help)
        echo "Mystcraft Test Runner"
        echo ""
        echo "Usage: ./run-tests.sh [options]"
        echo ""
        echo "Options:"
        echo "  -h, --help     Show this help message"
        echo "  --clean        Run clean before tests"
        echo "  --fabric       Run only Fabric tests"
        echo "  --forge        Run only Forge tests"
        echo "  --neoforge     Run only NeoForge tests"
        echo ""
        echo "Output:"
        echo "  Logs are saved to test-logs/ directory"
        exit 0
        ;;
    --clean)
        ./gradlew clean
        main
        ;;
    --fabric)
        cd "$SCRIPT_DIR"
        print_header | tee "$LOG_FILE"
        run_module_tests ":fabric:1.18.2:runGametest" "Fabric 1.18.2"
        run_module_tests ":fabric:1.19.2:runGametest" "Fabric 1.19.2"
        run_module_tests ":fabric:1.20.1:runGametest" "Fabric 1.20.1"
        run_module_tests ":fabric:1.20.2:runGametest" "Fabric 1.20.2"
        print_summary
        ;;
    --forge)
        cd "$SCRIPT_DIR"
        print_header | tee "$LOG_FILE"
        run_module_tests ":forge:1.18.2:runGametest" "Forge 1.18.2"
        run_module_tests ":forge:1.19.2:runGametest" "Forge 1.19.2"
        run_module_tests ":forge:1.20.1:runGametest" "Forge 1.20.1"
        run_module_tests ":forge:1.20.2:runGametest" "Forge 1.20.2"
        print_summary
        ;;
    --neoforge)
        cd "$SCRIPT_DIR"
        print_header | tee "$LOG_FILE"
        run_module_tests ":neoforge:1.20.2:runGametest" "NeoForge 1.20.2"
        print_summary
        ;;
    *)
        main
        ;;
esac
