#!/usr/bin/env bash
#
# Offer records up for rating again.
#
#   ./feed.sh            all five sources
#   ./feed.sh billing    the billing system's own CDRs
#   ./feed.sh call       the probe's call records
#   ./feed.sh sms|ftp|http   likewise
#
# Unlike the file based examples, nothing is fed in from outside here: the
# records are already in the database, and a flag on each one says whether it
# has been rated. The input adapters poll for records where that flag is
# unset, so clearing it is what gives the framework work to do.
#
#   mtn_billing_cdr     FIELD3 is null          -> not yet rated
#   xmass_or_cdr_*      OR_RATE_CONTROL = 0     -> not yet rated
#
# The matching result rows are deleted at the same time, so the counts in
# ./db.sh status stay honest.

set -euo pipefail
cd "$(dirname "$0")"
. ./common.sh

check_db_tooling
db_answering || fail "No MySQL answering on ${DB_HOST}:${DB_PORT}. Run ./db.sh start first."

reset_billing() {
  db_mysql "$CDR_DB" <<'SQL'
update mtn_billing_cdr set FIELD3 = null;
delete from or_billing_result;
SQL
}

reset_probe() {
  local t="$1"
  db_mysql "$CDR_DB" <<SQL
update xmass_or_cdr_${t} set OR_RATE_CONTROL = 0;
delete from or_xmass_${t}_result;
SQL
}

case "${1:-all}" in
  all)
    step "Offering every record up for rating again"
    reset_billing
    for t in call sms ftp http; do reset_probe "$t"; done
    ;;
  billing)
    step "Offering the billing CDRs up for rating again"
    reset_billing
    ;;
  call|sms|ftp|http)
    step "Offering the probe ${1} records up for rating again"
    reset_probe "$1"
    ;;
  *)
    fail "Usage: $0 [all|billing|call|sms|ftp|http]"
    ;;
esac

say "    $(db_pending) record(s) now waiting to be rated."
say ""

if framework_running; then
  say "The framework is running, so it will pick them up on its next poll."
  say "Watch the result with:  ./db.sh status"
else
  say "Note: nothing is listening on ${ECI_HOST}:${ECI_PORT}, so the framework does"
  say "      not look like it is running. The records will be picked up when you"
  say "      start it with ./run.sh"
fi
