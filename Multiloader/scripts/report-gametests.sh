#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out="$root_dir/logs/gametest-report.txt"

fabric_run_log="$root_dir/fabric/runs/gametest/logs/latest.log"
forge_run_log="$root_dir/forge/runs/gametest/logs/latest.log"
neoforge_run_log="$root_dir/neoforge/runs/gametest/logs/latest.log"

write_section() {
  local label="$1"
  local run_log="$2"

  echo "  ${label}:"
  if [[ ! -f "$run_log" ]]; then
    echo "    - STATUS: MISSING LOG"
    return
  fi

  if grep -q "required tests failed" "$run_log"; then
    echo "    - STATUS: FAIL"
    grep -F "   - " "$run_log" | sed 's/^/    /'
  else
    echo "    - STATUS: PASS"
  fi
}

{
  echo "Mystcraft GameTest Report"
  echo "Generated: $(date)"
  echo
  echo "Tests"
  write_section "Fabric" "$fabric_run_log"
  write_section "Forge" "$forge_run_log"
  write_section "NeoForge" "$neoforge_run_log"
  echo
  echo "Report written to logs/gametest-report.txt"
} >"$out"

echo "Report written to $out"
