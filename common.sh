#!/usr/bin/env bash
#
# Shared helpers for the ORPixip scripts.
# Not meant to be run directly.

REQUIRED_JAVA=17
ECI_HOST=localhost
ECI_PORT=8086

# This application uses two schemas on one MySQL server:
#   PixipDB  the rating reference data - zones, time models, prices
#   mtn_poc  the operator's own data - the CDRs to rate, and the results
# The connection details are hard coded in
# src/main/resources/Pixip.properties.xml, so the defaults here have to match
# what is configured there.
CONFIG_DB=PixipDB
CDR_DB=mtn_poc
DB_HOST="${OPENRATE_DB_HOST:-localhost}"
DB_PORT="${OPENRATE_DB_PORT:-3307}"

# The scripts administer the server as root, but the application connects as
# its own user, which db.sh creates. Both are in the properties file.
DB_USER="${OPENRATE_DB_USER:-root}"
DB_PASS="${OPENRATE_DB_PASS:-cpr}"
APP_USER=openrate
APP_PASS=openrate

# docker - db.sh runs MySQL in a container (the default, nothing to install)
# local  - db.sh talks to a MySQL you already run, using the mysql client
DB_MODE="${OPENRATE_DB_MODE:-docker}"
DB_CONTAINER="${OPENRATE_DB_CONTAINER:-openrate-pixip-db}"
DB_IMAGE="${OPENRATE_DB_IMAGE:-mysql:8.4}"

say()  { printf '%s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

step() {
  printf '\n\033[1m==> %s\033[0m\n' "$*"
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

# Exit unless java >= REQUIRED_JAVA and maven are both available.
check_toolchain() {
  command -v java >/dev/null 2>&1 || fail \
    "No 'java' on the PATH. This needs JDK ${REQUIRED_JAVA} or later.
       macOS:  brew install --cask temurin@${REQUIRED_JAVA}"
  command -v mvn >/dev/null 2>&1 || fail \
    "No 'mvn' on the PATH.
       macOS:  brew install maven"

  local raw major
  raw=$(java -version 2>&1 | head -1 | sed -n 's/.*version "\([^"]*\)".*/\1/p')
  case "$raw" in
    1.*) major=$(printf '%s' "$raw" | cut -d. -f2) ;;
    *)   major=$(printf '%s' "$raw" | cut -d. -f1) ;;
  esac
  if [ -z "$major" ] || [ "$major" -lt "$REQUIRED_JAVA" ]; then
    fail "Java ${raw:-unknown} found, but ${REQUIRED_JAVA} or later is needed.
       macOS:  brew install --cask temurin@${REQUIRED_JAVA}
       then:   export JAVA_HOME=\$(/usr/libexec/java_home -v ${REQUIRED_JAVA})"
  fi
}

# The OpenRate version this application depends on, read from the pom.
openrate_version() {
  sed -n '/<artifactId>OpenRate<\/artifactId>/,/<\/dependency>/p' pom.xml \
    | sed -n 's|.*<version>\(.*\)</version>.*|\1|p' | head -1
}

# Exit unless the OpenRate framework jar is in the local Maven repository.
check_openrate_installed() {
  local v repo
  v=$(openrate_version)
  [ -n "$v" ] || fail "Could not read the OpenRate version from pom.xml"
  repo="${HOME}/.m2/repository/OpenRate/OpenRate/${v}/OpenRate-${v}.jar"

  if [ ! -f "$repo" ]; then
    fail "OpenRate ${v} is not installed in your local Maven repository.

OpenRate is not published to Maven Central, so the framework has to be
built and installed before this application can resolve it:

    git clone https://github.com/isparkes/OpenRate.git
    cd OpenRate
    ./bootstrap.sh"
  fi
}

# ---------------------------------------------------------------- database --

# Exit unless the tooling for the selected DB_MODE is present.
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

# True while the MySQL container is up (docker mode only).
db_container_running() {
  [ "$(docker inspect -f '{{.State.Running}}' "$DB_CONTAINER" 2>/dev/null)" = "true" ]
}

# Run a mysql client command as the administrative user. Reads SQL from
# stdin; any extra arguments are passed through to the client, so the schema
# to work in is given as the last argument.
db_mysql() {
  if [ "$DB_MODE" = docker ]; then
    # Connect over TCP rather than the socket: while the image is still
    # initialising it runs a temporary server with networking disabled, and
    # connecting to that one would make the database look ready too early.
    docker exec -i -e MYSQL_PWD="$DB_PASS" "$DB_CONTAINER" \
      mysql --protocol=tcp --host=127.0.0.1 --user="$DB_USER" "$@"
  else
    MYSQL_PWD="$DB_PASS" mysql --host="$DB_HOST" --port="$DB_PORT" \
          --user="$DB_USER" "$@"
  fi
}

# One scalar out of a query, or nothing if it could not be run.
db_scalar() {
  local schema="$1" query="$2"
  printf '%s\n' "$query" | db_mysql --silent --skip-column-names "$schema" 2>/dev/null
}

# True once the server answers a trivial query.
db_answering() {
  printf 'select 1;\n' | db_mysql --silent >/dev/null 2>&1
}

# True once both schemas are loaded: the reference data to rate against, and
# the operator data to rate.
db_loaded() {
  local prices cdrs
  prices=$(db_scalar "$CONFIG_DB" 'select count(*) from PRICE_MODEL;') || return 1
  cdrs=$(db_scalar "$CDR_DB" 'select count(*) from mtn_billing_cdr;')  || return 1
  [ -n "$prices" ] && [ -n "$cdrs" ] \
    && [ "$prices" -gt 0 ] 2>/dev/null && [ "$cdrs" -gt 0 ] 2>/dev/null
}

# Exit unless the database is up and loaded.
check_database() {
  check_db_tooling

  if [ "$DB_MODE" = docker ] && ! db_container_running; then
    fail "The MySQL container '${DB_CONTAINER}' is not running.
       Start it and load the schemas with:  ./db.sh start"
  fi

  db_answering || fail \
    "No MySQL answering on ${DB_HOST}:${DB_PORT}.
       Start it and load the schemas with:  ./db.sh start"

  db_loaded || fail \
    "MySQL is up, but ${CONFIG_DB}/${CDR_DB} are not loaded.
       Load them with:  ./db.sh load"
}

# The rating state lives in the input tables themselves, in FIELD3 on a
# billing record and in OR_RATE_CONTROL on a probe record:
#
#   null / 0   waiting to be rated - this is what the input adapters poll for
#   1          claimed by an input adapter, in the pipeline
#   2          rated, waiting for the transaction to commit
#   3          done
#   -1         the transaction was rolled back; the record was not rated
#
# db_count_state totals one of those states across all five input tables.
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

# Waiting to be rated.
db_pending() {
  db_count_state "is null" "= 0"
}

# Claimed by a pipeline but not yet committed. A record stops being pending
# the moment an input adapter claims it, which is long before it has been
# rated, so anything waiting for a run to finish has to watch this too.
db_inflight() {
  db_count_state "in ('1','2')" "in (1,2)"
}

# Records whose transaction was rolled back. ./feed.sh offers them up again.
db_failed() {
  db_count_state "= '-1'" "= -1"
}

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

# ----------------------------------------------------------------- control --

# True while the framework's admin port is accepting connections.
framework_running() {
  port_open "$ECI_HOST" "$ECI_PORT"
}

# Send one command to the ECI admin console and echo the reply.
eci() {
  local cmd="$1"
  if command -v nc >/dev/null 2>&1; then
    { printf '%s\n' "$cmd"; sleep 2; } | nc "$ECI_HOST" "$ECI_PORT" 2>/dev/null
  else
    exec 3<>"/dev/tcp/${ECI_HOST}/${ECI_PORT}" || return 1
    printf '%s\n' "$cmd" >&3
    sleep 2
    cat <&3 &
    local pid=$!
    sleep 1; kill "$pid" 2>/dev/null || true
    exec 3<&-; exec 3>&-
  fi
}

# The working directories the framework writes into. Nothing here is input -
# the records come from the database - but the dump module and the logs need
# somewhere to go.
ensure_data_dirs() {
  mkdir -p Data/Pixip log Semaphore
}
