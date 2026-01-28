#!/usr/bin/env bash
set -u

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"

rm -rf logs
mkdir -p logs

ensure_empty_structure() {
  local run_dir="$1"
  local out_dir="${run_dir}/gameteststructures"
  mkdir -p "$out_dir"
  cat >"${out_dir}/empty.snbt" <<'SNBT'
{
  "size": [1, 1, 1],
  "palette": [
    {
      "Name": "minecraft:air"
    }
  ],
  "blocks": [
    {
      "pos": [0, 0, 0],
      "state": 0
    }
  ],
  "entities": []
}
SNBT
}

ensure_empty_structure "forge/runs/gametest"
ensure_empty_structure "neoforge/runs/gametest"

declare -a pids=()

run_task() {
  local name="$1"
  local task="$2"
  local log="logs/${name}.log"
  echo "Starting ${name}..."
  ./gradlew "$task" >"$log" 2>&1 &
  pids+=($!)
}

run_task "fabric-gametest" ":fabric:runGametest"
run_task "forge-gametest" ":forge:runGameTestServer"
run_task "neoforge-gametest" ":neoforge:runGameTestServer"

status=0
for pid in "${pids[@]}"; do
  if ! wait "$pid"; then
    status=1
  fi
done

bash scripts/report-gametests.sh || true

if [ "$status" -ne 0 ]; then
  echo "One or more GameTest runs failed. Check logs/*.log for details."
else
  echo "All GameTest runs completed successfully."
fi

exit "$status"
