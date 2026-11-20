#!/usr/bin/env bash
#
# Start the Pixip revenue assurance framework in the foreground.
#
# It runs as a daemon: it starts, polls the database for records that have
# not been rated yet, rates them, and keeps running until told to stop. Use
# ./feed.sh and ./stop.sh from another shell.

set -euo pipefail
cd "$(dirname "$0")"
. ./common.sh

check_toolchain
check_openrate_installed
check_database
ensure_data_dirs

if framework_running; then
  fail "Something is already listening on ${ECI_HOST}:${ECI_PORT}.
       The framework may already be running - try ./stop.sh first."
fi

cat <<BANNER

--------------------------------------------------------------
 Starting ORPixip
--------------------------------------------------------------
 Databases ${CONFIG_DB} (rates) and ${CDR_DB} (traffic)
           on ${DB_HOST}:${DB_PORT}
 Pipelines PixipPipe       rates the billing system's own CDRs
           XMASSCallPipe   rates the probe's call records
           XMASSSmsPipe    rates the probe's SMS records
           XMASSFtpPipe    rates the probe's FTP records
           XMASSHttpPipe   rates the probe's HTTP records

 Each pipeline reads its input table, rates what it finds, and
 writes the result next to the original record so the two can be
 compared. That comparison is the point: this is a revenue
 assurance application, not a billing one.

 $(db_pending) record(s) are waiting to be rated right now.

 The framework runs until you stop it. Once you see "Running...",
 open a second terminal in this directory and:

     ./db.sh status show what has been rated
     ./feed.sh      offer the records up for rating again
     ./stop.sh      shut it down cleanly

 Admin console:  telnet ${ECI_HOST} ${ECI_PORT}   ('Help' for commands)
--------------------------------------------------------------

BANNER

# Relative paths in Pixip.properties.xml resolve against the project root,
# so this must run from here.
exec mvn -B exec:java
