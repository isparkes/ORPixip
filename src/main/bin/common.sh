#!/usr/bin/env bash
#
# Shared helpers for the ORPixip distribution scripts.
# Not meant to be run directly.

REQUIRED_JAVA=17
ECI_HOST="${OPENRATE_ECI_HOST:-localhost}"
ECI_PORT="${OPENRATE_ECI_PORT:-8086}"

# This application uses two schemas on one MySQL server:
#   PixipDB  the rating reference data - zones, time models, prices
#   mtn_poc  the operator's own data - the CDRs to rate, and the results
# The connection details are hard coded in properties/Pixip.properties.xml,
# so the defaults here have to match what is configured there.
CONFIG_DB=PixipDB
CDR_DB=mtn_poc
DB_HOST="${OPENRATE_DB_HOST:-localhost}"
DB_PORT="${OPENRATE_DB_PORT:-3307}"

# The scripts administer the server as root; the application connects as its
# own user, which bin/db.sh creates.
DB_USER="${OPENRATE_DB_USER:-root}"
DB_PASS="${OPENRATE_DB_PASS:-cpr}"
APP_USER=openrate
APP_PASS=openrate

# docker - bin/db.sh runs MySQL in a container (the default)
# local  - bin/db.sh talks to a MySQL you already run, using the mysql client
DB_MODE="${OPENRATE_DB_MODE:-docker}"
DB_CONTAINER="${OPENRATE_DB_CONTAINER:-openrate-pixip-db}"
DB_IMAGE="${OPENRATE_DB_IMAGE:-mysql:8.4}"

say()  { printf '%s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

step() {
  printf '\n\033[1m==> %s\033[0m\n' "$*"
}

# Absolute path of the installation root (the parent of bin/).
openrate_home() {
  local here
  here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  dirname "$here"
}

# Locate a JDK of at least REQUIRED_JAVA, honouring JAVA_HOME if it is set.
resolve_java() {
  local candidate
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    candidate="${JAVA_HOME}/bin/java"
  elif command -v java >/dev/null 2>&1; then
    candidate="$(command -v java)"
  else
    fail "No Java found. OpenRate needs JDK ${REQUIRED_JAVA} or later.
       Set JAVA_HOME, or install one:
         macOS:  brew install --cask temurin@${REQUIRED_JAVA}
         Linux:  apt install openjdk-${REQUIRED_JAVA}-jdk"
  fi

  local raw major
  raw=$("$candidate" -version 2>&1 | head -1 | sed -n 's/.*version "\([^"]*\)".*/\1/p')
  case "$raw" in
    1.*) major=$(printf '%s' "$raw" | cut -d. -f2) ;;
    *)   major=$(printf '%s' "$raw" | cut -d. -f1) ;;
  esac
  if [ -z "$major" ] || [ "$major" -lt "$REQUIRED_JAVA" ]; then
    fail "Found Java ${raw:-unknown} at ${candidate}, but ${REQUIRED_JAVA} or later is needed.
       Point JAVA_HOME at a newer JDK, for example:
         export JAVA_HOME=\$(/usr/libexec/java_home -v ${REQUIRED_JAVA})   # macOS"
  fi
  printf '%s' "$candidate"
}

# The classpath for the unpacked distribution.
#   properties  holds the configuration, which OpenRate loads from the classpath
#   dist        this application
#   ORlib       the OpenRate framework
#   lib         third party dependencies
openrate_classpath() {
  printf '%s' "properties:dist/*:ORlib/*:lib/*"
}

# True while something is accepting connections on host:port.
port_open() {
  local host="$1" port="$2"
  if command -v nc >/dev/null 2>&1; then
    nc -z "$host" "$port" >/dev/null 2>&1
  else
    (exec 3<>"/dev/tcp/${host}/${port}") >/dev/null 2>&1
  fi
}

framework_running() {
  port_open "$ECI_HOST" "$ECI_PORT"
}

# Send one command to the admin console and echo the reply.
eci() {
  local cmd="$1"
  command -v nc >/dev/null 2>&1 || fail \
    "'nc' is not available, so this script cannot reach the admin console.
       Connect by hand instead:  telnet ${ECI_HOST} ${ECI_PORT}"
  { printf '%s\n' "$cmd"; sleep 2; } | nc "$ECI_HOST" "$ECI_PORT" 2>/dev/null
}

# ---------------------------------------------------------------- database --

check_db_tooling() {
  case "$DB_MODE" in
    docker)
      command -v docker >/dev/null 2>&1 || fail \
        "No 'docker' on the PATH, and OPENRATE_DB_MODE is 'docker'.
       Install Docker, or point the scripts at a MySQL you already run:
           export OPENRATE_DB_MODE=local"
      docker info >/dev/null 2>&1 || fail \
        "Docker is installed but not running. Start Docker and try again."
      ;;
    local)
      command -v mysql >/dev/null 2>&1 || fail \
        "No 'mysql' client on the PATH, and OPENRATE_DB_MODE is 'local'.
       Install the MySQL client, or let the scripts run MySQL for you:
           export OPENRATE_DB_MODE=docker"
      ;;
    *) fail "OPENRATE_DB_MODE must be 'docker' or 'local', not '${DB_MODE}'" ;;
  esac
}

db_container_running() {
  [ "$(docker inspect -f '{{.State.Running}}' "$DB_CONTAINER" 2>/dev/null)" = "true" ]
}

# Run a mysql client command as the administrative user, reading SQL from
# stdin. Connects over TCP in both modes: while the container image is
# initialising it runs a temporary server with networking disabled, and
# connecting to that one would make the database look ready too early.
db_mysql() {
  if [ "$DB_MODE" = docker ]; then
    docker exec -i -e MYSQL_PWD="$DB_PASS" "$DB_CONTAINER" \
      mysql --protocol=tcp --host=127.0.0.1 --user="$DB_USER" "$@"
  else
    MYSQL_PWD="$DB_PASS" mysql --host="$DB_HOST" --port="$DB_PORT" \
          --user="$DB_USER" "$@"
  fi
}

db_scalar() {
  local schema="$1" query="$2"
  printf '%s\n' "$query" | db_mysql --silent --skip-column-names "$schema" 2>/dev/null
}

db_answering() {
  printf 'select 1;\n' | db_mysql --silent >/dev/null 2>&1
}

db_loaded() {
  local prices cdrs
  prices=$(db_scalar "$CONFIG_DB" 'select count(*) from PRICE_MODEL;') || return 1
  cdrs=$(db_scalar "$CDR_DB" 'select count(*) from mtn_billing_cdr;')  || return 1
  [ -n "$prices" ] && [ -n "$cdrs" ] \
    && [ "$prices" -gt 0 ] 2>/dev/null && [ "$cdrs" -gt 0 ] 2>/dev/null
}

check_database() {
  check_db_tooling

  if [ "$DB_MODE" = docker ] && ! db_container_running; then
    fail "The MySQL container '${DB_CONTAINER}' is not running.
       Start it and load the schemas with:  bin/db.sh start"
  fi

  db_answering || fail \
    "No MySQL answering on ${DB_HOST}:${DB_PORT}.
       Start it and load the schemas with:  bin/db.sh start"

  db_loaded || fail \
    "MySQL is up, but ${CONFIG_DB}/${CDR_DB} are not loaded.
       Load them with:  bin/db.sh load"
}

# The rating state lives in the input tables themselves, in FIELD3 on a
# billing record and in OR_RATE_CONTROL on a probe record:
#
#   null / 0   waiting to be rated - this is what the input adapters poll for
#   1          claimed by an input adapter, in the pipeline
#   2          rated, waiting for the transaction to commit
#   3          done
#   -1         the transaction was rolled back; the record was not rated
db_count_state() {
  local billing="$1" probe="$2" total=0 n t
  n=$(db_scalar "$CDR_DB" "select count(*) from mtn_billing_cdr where FIELD3 ${billing};")
  total=$((total + ${n:-0}))
  for t in call sms ftp http; do
    n=$(db_scalar "$CDR_DB" "select count(*) from xmass_or_cdr_${t} where OR_RATE_CONTROL ${probe};")
    total=$((total + ${n:-0}))
  done
  printf '%s' "$total"
}

db_pending()  { db_count_state "is null"       "= 0"; }
db_inflight() { db_count_state "in ('1','2')"  "in (1,2)"; }
db_failed()   { db_count_state "= '-1'"        "= -1"; }

# How many records have been rated, across all five result tables.
db_rated() {
  local total=0 n t
  n=$(db_scalar "$CDR_DB" 'select count(*) from or_billing_result;')
  total=$((total + ${n:-0}))
  for t in call sms ftp http; do
    n=$(db_scalar "$CDR_DB" "select count(*) from or_xmass_${t}_result;")
    total=$((total + ${n:-0}))
  done
  printf '%s' "$total"
}

# The framework needs these to exist; nothing here is input, because the
# records come from the database.
ensure_data_dirs() {
  mkdir -p Data/Pixip log Semaphore
}
