#!/bin/bash

# Mystcraft Test Runner - Minecraft 1.20.1 only

set -o pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REQUESTED_SUITE="${1:-fast}"

ALL_SUITES=(core book_travel book_crafting age_creation world_rules commands)
FAST_SUITES=(core book_travel book_crafting age_creation)
SELECTED_SUITES=()

PLATFORM_KEYS=(fabric forge)
PLATFORM_NAMES=(Fabric Forge)
PLATFORM_VERSIONS=("1.20.1" "1.20.1")
PLATFORM_LABELS=("Fabric 1.20.1" "Forge 1.20.1")
PLATFORM_TASKS=(":fabric:1.20.1:runGametest" ":forge:1.20.1:runGametest")
PLATFORM_STATUS=(PENDING PENDING)
PLATFORM_PASSED=(0 0)
PLATFORM_REPORTED=(0 0)
PLATFORM_DETAILS=("" "")

usage() {
  echo "Usage: ./run-tests.sh [fast|full|all|core|book_travel|book_crafting|age_creation|world_rules|commands|suite,suite] [gradle args...]"
  echo ""
  echo "Default: fast"
  echo "fast: core, book_travel, book_crafting, age_creation"
  echo "full/all: every suite, including commands and world_rules"
  echo ""
  echo "Policy: any GameTest failure, skipped test, ignored test, disabled test, fatal server log marker, or missing pass summary fails the run."
}

suite_count() {
  case "$1" in
    core) echo 1 ;;
    book_travel) echo 5 ;;
    book_crafting) echo 6 ;;
    age_creation) echo 5 ;;
    world_rules) echo 2 ;;
    commands) echo 1 ;;
    *) echo 0 ;;
  esac
}

is_known_suite() {
  local suite="$1"
  local known
  for known in "${ALL_SUITES[@]}"; do
    if [[ "$known" == "$suite" ]]; then
      return 0
    fi
  done
  return 1
}

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  echo "$value"
}

select_suites() {
  local selection
  selection="$(echo "$1" | tr '[:upper:]' '[:lower:]')"
  SELECTED_SUITES=()

  case "$selection" in
    fast|"")
      SELECTED_SUITES=("${FAST_SUITES[@]}")
      ;;
    full|all)
      SELECTED_SUITES=("${ALL_SUITES[@]}")
      ;;
    -h|--help|help)
      usage
      exit 0
      ;;
    *)
      local IFS=','
      local requested
      local suite
      read -ra requested <<< "$selection"
      for suite in "${requested[@]}"; do
        suite="$(trim "$suite")"
        if [[ -z "$suite" ]]; then
          continue
        fi
        if ! is_known_suite "$suite"; then
          echo -e "${RED}Unknown test suite: ${suite}${NC}"
          echo "Run './run-tests.sh --help' for valid suites."
          exit 1
        fi
        SELECTED_SUITES+=("$suite")
      done
      if [[ ${#SELECTED_SUITES[@]} -eq 0 ]]; then
        echo -e "${RED}No suites selected.${NC}"
        exit 1
      fi
      ;;
  esac
}

selected_test_count() {
  local total=0
  local suite
  for suite in "${SELECTED_SUITES[@]}"; do
    total=$((total + $(suite_count "$suite")))
  done
  echo "$total"
}

suite_list() {
  local output=""
  local suite
  for suite in "${SELECTED_SUITES[@]}"; do
    if [[ -n "$output" ]]; then
      output+=", "
    fi
    output+="${suite}($(suite_count "$suite"))"
  done
  echo "$output"
}

progress_bar() {
  local passed="$1"
  local total="$2"
  local status="$3"
  local width=24
  local filled=0
  local i
  local bar=""

  if [[ "$total" -gt 0 ]]; then
    filled=$((passed * width / total))
  fi
  if [[ "$status" == "FAIL" ]]; then
    filled="$width"
  elif [[ "$status" == "PENDING" ]]; then
    filled=0
  fi

  for ((i = 0; i < width; i++)); do
    if [[ "$i" -lt "$filled" ]]; then
      if [[ "$status" == "FAIL" ]]; then
        bar+="!"
      else
        bar+="#"
      fi
    else
      bar+="."
    fi
  done
  echo "$bar"
}

status_color() {
  case "$1" in
    PASS) echo "$GREEN" ;;
    FAIL) echo "$RED" ;;
    RUNNING) echo "$YELLOW" ;;
    *) echo "$CYAN" ;;
  esac
}

print_graph() {
  local expected="$1"
  local total_expected=$((expected * ${#PLATFORM_KEYS[@]}))
  local total_passed=0
  local total_failed=0
  local index

  for ((index = 0; index < ${#PLATFORM_KEYS[@]}; index++)); do
    case "${PLATFORM_STATUS[$index]}" in
      PASS) total_passed=$((total_passed + expected)) ;;
      FAIL) total_failed=$((total_failed + expected)) ;;
    esac
  done

  echo ""
  echo -e "${BOLD}Test Status Graph${NC}"
  echo "Selected suites: $(suite_list)"
  echo "Policy: fail on failed, skipped, ignored, disabled, fatal server log markers, or missing GameTest summaries."
  printf "Overall  [%s] %d/%d passed, %d failed\n" \
    "$(progress_bar "$total_passed" "$total_expected" "PASS")" \
    "$total_passed" "$total_expected" "$total_failed"

  for ((index = 0; index < ${#PLATFORM_KEYS[@]}; index++)); do
    local status="${PLATFORM_STATUS[$index]}"
    local color
    color="$(status_color "$status")"
    local passed="${PLATFORM_PASSED[$index]}"
    local reported="${PLATFORM_REPORTED[$index]}"
    local bar
    bar="$(progress_bar "$passed" "$expected" "$status")"
    printf "%-13s [%s] %b%-7s%b selected %2d/%-2d reported %2d" \
      "${PLATFORM_LABELS[$index]}" "$bar" "$color" "$status" "$NC" "$passed" "$expected" "$reported"
    if [[ -n "${PLATFORM_DETAILS[$index]}" ]]; then
      printf "  %s" "${PLATFORM_DETAILS[$index]}"
    fi
    printf "\n"

    local suite
    for suite in "${SELECTED_SUITES[@]}"; do
      local count
      count="$(suite_count "$suite")"
      if [[ "$status" == "PASS" ]]; then
        printf "  %-18s %bPASS%b %2d/%-2d\n" "$suite" "$GREEN" "$NC" "$count" "$count"
      elif [[ "$status" == "FAIL" ]]; then
        printf "  %-18s %bFAIL%b %2d/%-2d\n" "$suite" "$RED" "$NC" 0 "$count"
      elif [[ "$status" == "RUNNING" ]]; then
        printf "  %-18s %bRUNNING%b\n" "$suite" "$YELLOW" "$NC"
      else
        printf "  %-18s PENDING\n" "$suite"
      fi
    done
  done
  echo ""
}

print_final_summary() {
  local expected="$1"
  local final_status="$2"
  local index

  echo ""
  echo -e "${BOLD}Final Platform Statuses${NC}"
  echo "Selected suites: $(suite_list)"
  echo "Policy: fail on failed, skipped, ignored, disabled, fatal server log markers, or missing GameTest summaries."
  printf "%-10s %-9s %-8s %-15s %-10s %s\n" \
    "Platform" "Version" "Status" "Selected" "Reported" "Details"
  printf "%-10s %-9s %-8s %-15s %-10s %s\n" \
    "--------" "-------" "------" "--------" "--------" "-------"

  for ((index = 0; index < ${#PLATFORM_KEYS[@]}; index++)); do
    local status="${PLATFORM_STATUS[$index]}"
    local color
    color="$(status_color "$status")"
    local passed="${PLATFORM_PASSED[$index]}"
    local reported="${PLATFORM_REPORTED[$index]}"
    local details="${PLATFORM_DETAILS[$index]}"
    if [[ -z "$details" ]]; then
      details="-"
    fi

    printf "%-10s %-9s %b%-8s%b %2d/%-11d %-10d %s\n" \
      "${PLATFORM_NAMES[$index]}" \
      "${PLATFORM_VERSIONS[$index]}" \
      "$color" "$status" "$NC" \
      "$passed" "$expected" "$reported" "$details"
  done

  echo ""
  if [[ "$final_status" -eq 0 ]]; then
    echo -e "${GREEN}Final result: PASS${NC}"
  else
    echo -e "${RED}Final result: FAIL${NC}"
  fi
}

skip_pattern() {
  echo '(<skipped|GameTest[^[:alnum:]]+.*(skip|skipped|ignored|disabled)|game test[^[:alnum:]]+.*(skip|skipped|ignored|disabled)|tests?[^[:alnum:]]+.*(skip|skipped|ignored|disabled)|(skip|skipped|ignored|disabled)[^[:alnum:]]+.*(GameTest|game test|tests?))'
}

fatal_log_pattern() {
  echo '(POI data mismatch|Exception stopping the server|Game test server crashed|FAILED REQUIRED TEST)'
}

clean_gametest_worlds() {
  local paths=(
    "${SCRIPT_DIR}/fabric/1.20.1/runs/gametest/world"
    "${SCRIPT_DIR}/forge/1.20.1/runs/gametest/world"
    "${SCRIPT_DIR}/forge/1.20.1/runs/world"
  )
  local path

  echo -e "${CYAN}Cleaning generated GameTest worlds${NC}"
  for path in "${paths[@]}"; do
    if [[ -e "$path" ]]; then
      rm -rf "$path"
      echo "  removed ${path}"
    else
      echo "  already clean ${path}"
    fi
  done
  echo ""
}

analyze_log() {
  local log_file="$1"
  local gradle_status="$2"
  local expected="$3"
  local passed_count
  local skip_count
  local fatal_count

  passed_count="$(sed -nE 's/.*All ([0-9]+) required tests passed.*/\1/p' "$log_file" | tail -n 1)"
  skip_count="$(grep -Eic "$(skip_pattern)" "$log_file" || true)"
  fatal_count="$(grep -Eic "$(fatal_log_pattern)" "$log_file" || true)"

  ANALYZED_REPORTED="${passed_count:-0}"
  ANALYZED_SELECTED_PASSED=0
  ANALYZED_STATUS=FAIL
  ANALYZED_DETAIL=""

  if [[ "$gradle_status" -ne 0 ]]; then
    ANALYZED_DETAIL="Gradle exited ${gradle_status}"
    return
  fi

  if [[ -z "$passed_count" ]]; then
    ANALYZED_DETAIL="missing GameTest pass summary"
    return
  fi

  if [[ "$skip_count" -gt 0 ]]; then
    ANALYZED_DETAIL="${skip_count} skipped/ignored/disabled test marker(s)"
    return
  fi

  if [[ "$fatal_count" -gt 0 ]]; then
    ANALYZED_DETAIL="${fatal_count} fatal server log marker(s)"
    return
  fi

  local suite
  for suite in "${SELECTED_SUITES[@]}"; do
    local expected_suite_count
    local discovered_suite_count
    expected_suite_count="$(suite_count "$suite")"
    discovered_suite_count="$(sed -nE "s/.*Running test batch '${suite}(:[0-9]+)?' \\(([0-9]+) tests\\).*/\\2/p" "$log_file" | awk '{ total += $1 } END { print total + 0 }')"
    if [[ "$discovered_suite_count" -lt "$expected_suite_count" ]]; then
      ANALYZED_DETAIL="${suite} discovered ${discovered_suite_count}/${expected_suite_count} tests"
      return
    fi
  done

  if [[ "$passed_count" -lt "$expected" ]]; then
    ANALYZED_DETAIL="reported ${passed_count}, expected at least ${expected}"
    return
  fi

  ANALYZED_SELECTED_PASSED="$expected"
  ANALYZED_STATUS=PASS
}

run_platform() {
  local index="$1"
  local expected="$2"
  local key="${PLATFORM_KEYS[$index]}"
  local label="${PLATFORM_LABELS[$index]}"
  local task="${PLATFORM_TASKS[$index]}"
  local log_file="${LOG_DIR}/${key}.log"
  local gradle_status

  PLATFORM_STATUS[$index]=RUNNING
  PLATFORM_DETAILS[$index]="log: ${log_file}"
  print_graph "$expected"

  echo -e "${CYAN}Starting ${label}: ${task}${NC}"
  echo ""

  if [[ -n "${JAVA_17_HOME:-}" ]]; then
    JAVA_HOME="$JAVA_17_HOME" ./gradlew "$task" --no-daemon "${EXTRA_GRADLE_ARGS[@]}" 2>&1 | tee "$log_file"
    gradle_status=${PIPESTATUS[0]}
  else
    ./gradlew "$task" --no-daemon "${EXTRA_GRADLE_ARGS[@]}" 2>&1 | tee "$log_file"
    gradle_status=${PIPESTATUS[0]}
  fi

  analyze_log "$log_file" "$gradle_status" "$expected"

  PLATFORM_STATUS[$index]="$ANALYZED_STATUS"
  PLATFORM_PASSED[$index]="$ANALYZED_SELECTED_PASSED"
  PLATFORM_REPORTED[$index]="$ANALYZED_REPORTED"
  if [[ "$ANALYZED_STATUS" == "PASS" ]]; then
    PLATFORM_DETAILS[$index]="complete; log: ${log_file}"
  else
    PLATFORM_DETAILS[$index]="${ANALYZED_DETAIL}; log: ${log_file}"
  fi

  print_graph "$expected"
}

select_suites "$REQUESTED_SUITE"
shift || true
EXTRA_GRADLE_ARGS=("$@")

EXPECTED_PER_PLATFORM="$(selected_test_count)"

if [[ "$OSTYPE" == "darwin"* ]]; then
  JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
  if [[ -z "$JAVA_17_HOME" ]]; then
    echo -e "${RED}Error: Java 17 not found.${NC}"
    exit 1
  fi
fi

cd "$SCRIPT_DIR" || exit 1

export GRADLE_USER_HOME="${HOME}/.gradle"
export MYSTCRAFT_GAMETEST_SUITE="$REQUESTED_SUITE"

LOG_DIR="${SCRIPT_DIR}/build/test-runner/$(date +%Y%m%d-%H%M%S)-$(echo "$REQUESTED_SUITE" | tr -c '[:alnum:]_.-' '_')"
mkdir -p "$LOG_DIR"

echo -e "${CYAN}Running Mystcraft GameTests (${REQUESTED_SUITE})${NC}"
echo "Logs: ${LOG_DIR}"
echo ""

clean_gametest_worlds
print_graph "$EXPECTED_PER_PLATFORM"

for ((i = 0; i < ${#PLATFORM_KEYS[@]}; i++)); do
  run_platform "$i" "$EXPECTED_PER_PLATFORM"
done

FINAL_STATUS=0
for ((i = 0; i < ${#PLATFORM_KEYS[@]}; i++)); do
  if [[ "${PLATFORM_STATUS[$i]}" != "PASS" ]]; then
    FINAL_STATUS=1
  fi
done

if [[ "$FINAL_STATUS" -eq 0 ]]; then
  echo -e "${GREEN}Tests complete (${REQUESTED_SUITE})${NC}"
else
  echo -e "${RED}Tests failed (${REQUESTED_SUITE})${NC}"
fi

print_final_summary "$EXPECTED_PER_PLATFORM" "$FINAL_STATUS"
exit "$FINAL_STATUS"
