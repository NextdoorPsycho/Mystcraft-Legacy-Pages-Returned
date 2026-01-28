#!/usr/bin/env bash
set -u

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"

mkdir -p logs

declare -a pids=()

run_task() {
  local name="$1"
  local task="$2"
  local log="logs/${name}.log"
  echo "Starting ${name}..."
  ./gradlew "$task" >"$log" 2>&1 &
  pids+=($!)
}

run_task "fabric-client" ":fabric:runClient"
run_task "forge-client" ":forge:runClient"
run_task "neoforge-client" ":neoforge:runClient"

status=0
for pid in "${pids[@]}"; do
  if ! wait "$pid"; then
    status=1
  fi
done

if [ "$status" -ne 0 ]; then
  echo "One or more client runs failed. Check logs/*.log for details."
else
  echo "All client runs completed successfully."
fi

exit "$status"
