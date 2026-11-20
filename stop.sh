#!/usr/bin/env bash
#
# Shut the framework down cleanly through its admin console.
#
# The database is left running - use ./db.sh stop for that.

set -euo pipefail
cd "$(dirname "$0")"
. ./common.sh

if ! framework_running; then
  say "Nothing listening on ${ECI_HOST}:${ECI_PORT} - already stopped."
  exit 0
fi

step "Asking the framework to shut down"
eci 'Framework:shutdown=true' | sed 's/^/    /'

for _ in $(seq 1 30); do
  framework_running || { say ""; say "Stopped."; exit 0; }
  sleep 1
done

fail "Still listening on ${ECI_HOST}:${ECI_PORT} after 30s."
