#!/usr/bin/env bash
set -u

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$root_dir"

report="${script_dir}/gametest-report.txt"
: > "$report"

prop() {
  local key="$1"
  local file="gradle.properties"
  if [[ -f "$file" ]]; then
    rg -n "^${key}=" "$file" | head -n 1 | cut -d'=' -f2-
  fi
}

echo "Mystcraft GameTest Report" >> "$report"
echo "Generated: $(date)" >> "$report"
echo "" >> "$report"

echo "Versions" >> "$report"
echo "  minecraft_version: $(prop minecraft_version)" >> "$report"
echo "  fabric_version: $(prop fabric_version)" >> "$report"
echo "  fabric_loader_version: $(prop fabric_loader_version)" >> "$report"
echo "  forge_version: $(prop forge_version)" >> "$report"
echo "  neoforge_version: $(prop neoforge_version)" >> "$report"
echo "  java_version: $(prop java_version)" >> "$report"
echo "" >> "$report"

echo "Tests" >> "$report"

report_fabric() {
  local junit="fabric/build/junit.xml"
  if [[ ! -f "$junit" ]]; then
    echo "  Fabric: junit.xml not found" >> "$report"
    return
  fi
  echo "  Fabric:" >> "$report"
  rg -n "<testcase " "$junit" | while read -r line; do
    local name
    name="$(echo "$line" | sed -n 's/.*name="\\([^"]*\\)".*/\\1/p')"
    if echo "$line" | rg -q "<failure"; then
      local msg
      msg="$(echo "$line" | sed -n 's/.*failure message="\\([^"]*\\)".*/\\1/p')"
      echo "    - $name: FAIL ($msg)" >> "$report"
    else
      # Look ahead for a failure on the same test line (compact junit)
      if echo "$line" | rg -q "<failure"; then
        echo "    - $name: FAIL" >> "$report"
      else
        echo "    - $name: PASS" >> "$report"
      fi
    fi
  done
}

report_loader_from_log() {
  local label="$1"
  local log="$2"
  if [[ ! -f "$log" ]]; then
    echo "  ${label}: log not found" >> "$report"
    return
  fi
  echo "  ${label}:" >> "$report"
  local pass_count
  pass_count="$(rg -n "All [0-9]+ required tests passed" "$log" | tail -n 1 || true)"
  local fail_line
  fail_line="$(rg -n "required tests failed" "$log" | tail -n 1 || true)"
  if [[ -n "$fail_line" ]]; then
    echo "    - STATUS: FAIL" >> "$report"
    rg -n " - mystcraft" "$log" | tail -n 20 | sed 's/^/    /' >> "$report"
  elif [[ -n "$pass_count" ]]; then
    echo "    - STATUS: PASS" >> "$report"
    rg -n " - mystcraft" "$log" | tail -n 20 | sed 's/^/    /' >> "$report"
  else
    echo "    - STATUS: UNKNOWN" >> "$report"
  fi
}

report_fabric
report_loader_from_log "Forge" "logs/forge-gametest.log"
report_loader_from_log "NeoForge" "logs/neoforge-gametest.log"

echo "" >> "$report"
echo "Errors (all matched lines)" >> "$report"

collect_errors() {
  local label="$1"
  local log="$2"
  if [[ -f "$log" ]]; then
    local matches
    matches="$(rg -n "ERROR|Exception|FAILED|FATAL|SEVERE" "$log" || true)"
    if [[ -n "$matches" ]]; then
      echo "  ${label}:" >> "$report"
      echo "$matches" | sed 's/^/    /' >> "$report"
    fi
  fi
}

collect_errors "Fabric" "logs/fabric-gametest.log"
collect_errors "Forge" "logs/forge-gametest.log"
collect_errors "NeoForge" "logs/neoforge-gametest.log"

echo "" >> "$report"
echo "Report written to ${report}" >> "$report"

cat "$report"
