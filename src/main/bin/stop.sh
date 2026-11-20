#!/usr/bin/env bash
#
# Shut down a running ORPixip through its admin console.
#
# The database is left running - use bin/db.sh stop for that.

set -euo pipefail

. "$(cd "$(dirname "$0")" && pwd)/common.sh"
cd "$(openrate_home)"

if ! framework_running; then
  say "Nothing listening on ${ECI_HOST}:${ECI_PORT} - already stopped."
  exit 0
fi

say "Asking OpenRate to shut down..."
eci 'Framework:shutdown=true' | sed 's/^/    /'

for _ in $(seq 1 30); do
  framework_running || { say "Stopped."; exit 0; }
  sleep 1
done

fail "Still listening on ${ECI_HOST}:${ECI_PORT} after 30 seconds."
