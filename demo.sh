#!/usr/bin/env bash
#
# Zero to running: bring up the database, build the application, start it,
# let it rate every record in the operator's data, show what it found, and
# shut down again.
#
# Everything this does by hand is in db.sh / build.sh / run.sh / feed.sh /
# stop.sh.

set -euo pipefail
cd "$(dirname "$0")"
. ./common.sh

RUN_LOG=$(mktemp -t orpixip)
FRAMEWORK_PID=""

cleanup() {
  if [ -n "$FRAMEWORK_PID" ] && kill -0 "$FRAMEWORK_PID" 2>/dev/null; then
    say ""
    step "Shutting the framework down"
    eci 'Framework:shutdown=true' >/dev/null 2>&1 || true
    for _ in $(seq 1 20); do
      framework_running || break
      sleep 1
    done
    kill "$FRAMEWORK_PID" 2>/dev/null || true
    wait "$FRAMEWORK_PID" 2>/dev/null || true
    say "Stopped. The database is still running - ./db.sh stop shuts it down."
  fi
  rm -f "$RUN_LOG"
}
trap cleanup EXIT INT TERM

check_toolchain
check_openrate_installed
check_db_tooling

if framework_running; then
  fail "Something is already listening on ${ECI_HOST}:${ECI_PORT}.
       Run ./stop.sh first, then try again."
fi

# --------------------------------------------------------------- 1. database
step "1/6  Bringing up MySQL"
./db.sh start | sed 's/^/  /'

# ------------------------------------------------------------------ 2. build
step "2/6  Building against OpenRate $(openrate_version)"
mvn -B -q clean package
say "    Built."

# ------------------------------------------------------------------ 3. clean
step "3/6  Clearing the results of any previous run"
ensure_data_dirs
# The Dump module writes one line per record per module, so the dumps from a
# previous run are large and would be confusing to read alongside this one's.
rm -f Data/Pixip/*.dump
./feed.sh all | sed -n 's/^    /  /p'

# ------------------------------------------------------------------ 4. start
step "4/6  Starting the framework"
mvn -B exec:java > "$RUN_LOG" 2>&1 &
FRAMEWORK_PID=$!

for _ in $(seq 1 120); do
  grep -q 'Running\.\.\.' "$RUN_LOG" 2>/dev/null && break
  kill -0 "$FRAMEWORK_PID" 2>/dev/null || { sed 's/^/    /' "$RUN_LOG" | tail -20; fail "The framework exited during startup."; }
  sleep 1
done
grep -q 'Running\.\.\.' "$RUN_LOG" || { sed 's/^/    /' "$RUN_LOG" | tail -20; fail "The framework did not reach 'Running...' in 120s."; }

sed -n '/OpenRate V/,/Running\.\.\./p' "$RUN_LOG" \
  | grep -E "OpenRate V|Copyright|Creating pipeline|Running" | sed 's/^/    /'

# -------------------------------------------------------------------- 5. run
step "5/6  Rating everything in the operator's data"
say "    Nothing is fed in from outside: the records are already in the"
say "    database, and the five input adapters poll for the ones that have"
say "    not been rated yet."
say ""

expected=$(db_pending)
printf '    %s records to rate' "$expected"
for _ in $(seq 1 150); do
  # A record stops being pending as soon as a pipeline claims it, so wait
  # for the in flight ones to commit as well.
  [ "$(db_pending)" = "0" ] && [ "$(db_inflight)" = "0" ] && break
  printf '.'
  sleep 2
done
printf '\n'

if [ "$(db_pending)" != "0" ] || [ "$(db_inflight)" != "0" ]; then
  fail "$(db_pending) record(s) still waiting and $(db_inflight) still in flight
       after 5 minutes. Framework log:
$(tail -20 "$RUN_LOG")"
fi

say "    Rated $(db_rated)."
[ "$(db_failed)" = "0" ] || say "    $(db_failed) record(s) failed - see log/Error.log."

# ----------------------------------------------------------------- 6. result
step "6/6  What the rating found"
cat <<'EXPLAIN'

    Each pipeline read one source of records, worked out the customer's
    tariff, normalised the dialled number, zoned it, picked the time
    band and the price model, rated the record, and then compared the
    result with the charge the source system had applied.

    Agreement is the expected case. The records that matter are the ones
    where the two disagree by more than the tolerance in CompareCharge
    (8 cents on voice, 5 on everything else) - those are written with
    "#DIFF" on the end of their rating trail, and they are what a revenue
    assurance analyst would go and look at.

EXPLAIN
./db.sh status | sed -n '/^+/,$p' | sed 's/^/    /'

cat <<NEXT

--------------------------------------------------------------
 Next steps
--------------------------------------------------------------
  ./run.sh              start it and leave it running
  ./feed.sh             offer every record up for rating again
  ./feed.sh call        just the probe's call records
  ./db.sh status        show what the rating found
  ./db.sh shell         poke around the database yourself
  ./stop.sh             shut the framework down
  ./db.sh stop          shut the database down

  telnet ${ECI_HOST} ${ECI_PORT}    admin console, 'Help' for commands

  The rating reference data is in the ${CONFIG_DB} schema - change a
  price in PRICE_MODEL, run ./feed.sh, and the difference shows up
  in the next ./db.sh status.
--------------------------------------------------------------
NEXT
