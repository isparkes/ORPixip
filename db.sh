#!/usr/bin/env bash
#
# Bring up the MySQL server this application rates against, and load its two
# schemas: PixipDB (rating reference data) and mtn_poc (the operator's CDRs
# and the result tables).
#
#   ./db.sh            start MySQL if needed, then load the schemas (= start)
#   ./db.sh start      same
#   ./db.sh load       (re)load both schemas from the dumps in ConfigData
#   ./db.sh status     say whether the database is up, and show the results
#   ./db.sh shell      open a mysql prompt on mtn_poc
#   ./db.sh stop       stop the database
#   ./db.sh reset      throw the database away and start over
#
# By default MySQL runs in a Docker container on port 3307, so there is
# nothing to install and it does not collide with a MySQL of your own, or
# with the other OpenRate examples. To use a MySQL you already run instead:
#
#   export OPENRATE_DB_MODE=local
#   export OPENRATE_DB_USER=root OPENRATE_DB_PASS=yourpassword
#
# The connection details are hard coded in Pixip.properties.xml, so a server
# on another host or port needs that file changed to match.

set -euo pipefail
cd "$(dirname "$0")"
. ./common.sh

CONFIG_DUMP=ConfigData/Pixip/Pixip-012.sql.gz
CDR_DUMP=ConfigData/Pixip/mtn.sql.gz
CONFIG_PROCS=ConfigData/Pixip/SP.sql
CDR_PROCS=ConfigData/Pixip/SP_MTN_POC.sql

# The dumps were taken from MySQL 5.5 and the procedures carry DEFINER
# clauses naming users that do not exist on a fresh server. Strip them: the
# procedures then belong to whoever loads them.
strip_definers() {
  sed -e 's|/\*!50020 DEFINER=[^*]*\*/||g' -e 's|DEFINER=`[^`]*`@`[^`]*` ||g'
}

db_start() {
  if [ "$DB_MODE" = local ]; then
    db_answering || fail \
      "No MySQL answering on ${DB_HOST}:${DB_PORT} as user '${DB_USER}'.
       Start your own server, or let the scripts run one:
           export OPENRATE_DB_MODE=docker"
    say "    MySQL is answering on ${DB_HOST}:${DB_PORT}."
    return
  fi

  if db_container_running; then
    say "    Container '${DB_CONTAINER}' is already running."
  elif docker inspect "$DB_CONTAINER" >/dev/null 2>&1; then
    say "    Starting the existing container '${DB_CONTAINER}'."
    docker start "$DB_CONTAINER" >/dev/null
  else
    if port_open "$DB_HOST" "$DB_PORT"; then
      fail "Something is already listening on ${DB_HOST}:${DB_PORT}.
       That is probably a MySQL of your own. Either stop it, or use it:
           export OPENRATE_DB_MODE=local"
    fi
    say "    Creating container '${DB_CONTAINER}' from ${DB_IMAGE}."
    docker run --name "$DB_CONTAINER" \
      -e MYSQL_ROOT_PASSWORD="$DB_PASS" \
      -p "${DB_PORT}:3306" \
      -d "$DB_IMAGE" >/dev/null
  fi

  # The image initialises itself on first run, which takes a while and
  # involves a restart, so wait for the server to answer and stay answering.
  printf '    Waiting for MySQL to accept connections'
  local i settled=0
  for i in $(seq 1 90); do
    if port_open "$DB_HOST" "$DB_PORT" && db_answering; then
      settled=$((settled + 1))
      if [ "$settled" -ge 3 ]; then
        printf ' ok\n'
        return
      fi
    else
      settled=0
    fi
    db_container_running || { printf '\n'; docker logs --tail 20 "$DB_CONTAINER"; fail "The container stopped during startup."; }
    printf '.'
    sleep 1
  done
  printf '\n'
  fail "MySQL did not come up within 90s. Try:  docker logs ${DB_CONTAINER}"
}

db_load() {
  local f
  for f in "$CONFIG_DUMP" "$CDR_DUMP" "$CONFIG_PROCS" "$CDR_PROCS"; do
    [ -f "$f" ] || fail "Missing ${f}"
  done

  say "    Creating ${CONFIG_DB} and ${CDR_DB}, and the '${APP_USER}' user."
  db_mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${CONFIG_DB};
CREATE DATABASE IF NOT EXISTS ${CDR_DB};
CREATE USER IF NOT EXISTS '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASS}';
ALTER USER '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASS}';
GRANT ALL PRIVILEGES ON ${CONFIG_DB}.* TO '${APP_USER}'@'%';
GRANT ALL PRIVILEGES ON ${CDR_DB}.* TO '${APP_USER}'@'%';
GRANT EXECUTE ON *.* TO '${APP_USER}'@'%';
FLUSH PRIVILEGES;
SQL

  say "    Loading the rating reference data into ${CONFIG_DB}."
  gzip -dc "$CONFIG_DUMP" | db_mysql "$CONFIG_DB"
  strip_definers < "$CONFIG_PROCS" | db_mysql "$CONFIG_DB"

  say "    Loading the operator CDRs into ${CDR_DB}."
  gzip -dc "$CDR_DUMP" | db_mysql "$CDR_DB"
  strip_definers < "$CDR_PROCS" | db_mysql "$CDR_DB"

  # A record that cannot be rated is written to the result table with the
  # reason instead of an amount, and the framework's reasons are longer than
  # the varchar(255) the dump declares. Without this the output adapter
  # aborts the whole transaction on the first unrateable record.
  say "    Widening the result message columns."
  db_mysql "$CDR_DB" <<'SQL'
alter table or_billing_result    modify OR_MESSAGE varchar(1024);
alter table or_xmass_call_result modify OR_MESSAGE varchar(1024);
alter table or_xmass_sms_result  modify OR_MESSAGE varchar(1024);
alter table or_xmass_ftp_result  modify OR_MESSAGE varchar(1024);
alter table or_xmass_http_result modify OR_MESSAGE varchar(1024);
SQL

  # The dump ships the result tables already populated from a previous run.
  # Empty them, so a first run rates everything from scratch.
  say "    Clearing the result tables and marking every record unrated."
  db_reset_records

  say "    Loaded: $(db_scalar "$CONFIG_DB" 'select count(*) from PRICE_MODEL;') price model rows, \
$(db_scalar "$CONFIG_DB" 'select count(*) from DESTINATION_MAP;') destinations, \
$(db_pending) records waiting to be rated."
}

# Put every input record back into the "not yet rated" state and empty the
# result tables. This is what makes the example repeatable.
db_reset_records() {
  db_mysql "$CDR_DB" <<'SQL'
update mtn_billing_cdr  set FIELD3 = null;
update xmass_or_cdr_call set OR_RATE_CONTROL = 0;
update xmass_or_cdr_sms  set OR_RATE_CONTROL = 0;
update xmass_or_cdr_ftp  set OR_RATE_CONTROL = 0;
update xmass_or_cdr_http set OR_RATE_CONTROL = 0;
delete from or_billing_result;
delete from or_xmass_call_result;
delete from or_xmass_sms_result;
delete from or_xmass_ftp_result;
delete from or_xmass_http_result;
SQL
}

# What the rating found. A record whose OpenRate charge differs from the one
# the source system applied, by more than the tolerance in CompareCharge, is
# written with "#DIFF" on the end of its rating trail. Those are the records a
# revenue assurance analyst would look at.
db_show_results() {
  printf '%s\n' "
select 'billing'    as SOURCE, (select count(*) from mtn_billing_cdr)   as RECORDS, count(*) as RATED, sum(OR_MESSAGE like '%#DIFF%') as DIFFERS, round(sum(OR_RATED_AMOUNT),2) as RATED_VALUE from or_billing_result
union all select 'probe call', (select count(*) from xmass_or_cdr_call), count(*), sum(OR_MESSAGE like '%#DIFF%'), round(sum(OR_RATED_AMOUNT),2) from or_xmass_call_result
union all select 'probe sms',  (select count(*) from xmass_or_cdr_sms),  count(*), sum(OR_MESSAGE like '%#DIFF%'), round(sum(OR_RATED_AMOUNT),2) from or_xmass_sms_result
union all select 'probe ftp',  (select count(*) from xmass_or_cdr_ftp),  count(*), sum(OR_MESSAGE like '%#DIFF%'), round(sum(OR_RATED_AMOUNT),2) from or_xmass_ftp_result
union all select 'probe http', (select count(*) from xmass_or_cdr_http), count(*), sum(OR_MESSAGE like '%#DIFF%'), round(sum(OR_RATED_AMOUNT),2) from or_xmass_http_result;
" | db_mysql --table "$CDR_DB"
}

# The billing records OpenRate disagrees with, alongside what the billing
# system charged for them.
db_show_discrepancies() {
  printf '%s\n' "
select r.MTN_CDR_ID as CDR,
       c.TELESERVICE_CODE as SERVICE,
       c.CALL_DURATION as SECONDS,
       round(c.CHARGE_MAIN_ACCT + case when c.CHARGE_DA1 > 0 and not (c.TELESERVICE_CODE='VOICE' and c.CHARGE_DA1 = 70)
                                       then c.DA1_ACCT_BAL_BEFORE_CALL - c.DA1_ACCT_BAL_AFTER_CALL else 0 end, 2) as BILLED,
       r.OR_RATED_AMOUNT as OPENRATE,
       replace(r.OR_MESSAGE,',#DIFF','') as RATING_TRAIL
from or_billing_result r
join mtn_billing_cdr c on c.mtn_cdr_id = r.MTN_CDR_ID
where r.OR_MESSAGE like '%#DIFF%'
order by r.MTN_CDR_ID
limit 10;
" | db_mysql --table "$CDR_DB"
}

case "${1:-start}" in
  start)
    check_db_tooling
    step "Starting MySQL"
    db_start
    if db_loaded; then
      say "    ${CONFIG_DB} and ${CDR_DB} are already loaded - nothing to do."
    else
      step "Loading ${CONFIG_DB} and ${CDR_DB}"
      db_load
    fi
    say ""
    say "Database ready. Next:  ./build.sh  then  ./run.sh"
    ;;

  load)
    check_db_tooling
    db_answering || fail "No MySQL answering on ${DB_HOST}:${DB_PORT}. Run ./db.sh start first."
    step "Loading ${CONFIG_DB} and ${CDR_DB}"
    db_load
    ;;

  status)
    check_db_tooling
    if [ "$DB_MODE" = docker ] && ! db_container_running; then
      say "Container '${DB_CONTAINER}': not running"
    elif ! db_answering; then
      say "MySQL on ${DB_HOST}:${DB_PORT}: not answering"
    elif db_loaded; then
      say "MySQL on ${DB_HOST}:${DB_PORT}: up, ${CONFIG_DB} and ${CDR_DB} loaded"
      say "$(db_pending) record(s) waiting to be rated, $(db_rated) rated so far."
      [ "$(db_inflight)" = "0" ] || say "$(db_inflight) record(s) in flight right now."
      [ "$(db_failed)" = "0" ]   || say "$(db_failed) record(s) failed and were rolled back - ./feed.sh offers them up again."
      db_show_results
      if [ "$(db_scalar "$CDR_DB" "select count(*) from or_billing_result where OR_MESSAGE like '%#DIFF%';")" != "0" ]; then
        say ""
        say "Billing records OpenRate disagrees with:"
        db_show_discrepancies
      fi
    else
      say "MySQL on ${DB_HOST}:${DB_PORT}: up, but the schemas are not loaded. Run ./db.sh load"
    fi
    ;;

  shell)
    check_db_tooling
    if [ "$DB_MODE" = docker ]; then
      exec docker exec -it "$DB_CONTAINER" \
        mysql --user="$DB_USER" --password="$DB_PASS" "$CDR_DB"
    else
      exec mysql --host="$DB_HOST" --port="$DB_PORT" \
                 --user="$DB_USER" --password="$DB_PASS" "$CDR_DB"
    fi
    ;;

  stop)
    check_db_tooling
    if [ "$DB_MODE" = local ]; then
      say "OPENRATE_DB_MODE is 'local' - this script does not stop your own server."
      exit 0
    fi
    if db_container_running; then
      step "Stopping '${DB_CONTAINER}'"
      docker stop "$DB_CONTAINER" >/dev/null
      say "Stopped. The data is still there - ./db.sh start brings it back."
    else
      say "Container '${DB_CONTAINER}' is not running."
    fi
    ;;

  reset)
    check_db_tooling
    if [ "$DB_MODE" = local ]; then
      step "Dropping and reloading ${CONFIG_DB} and ${CDR_DB}"
      printf 'DROP DATABASE IF EXISTS %s;\nDROP DATABASE IF EXISTS %s;\n' \
        "$CONFIG_DB" "$CDR_DB" | db_mysql
      db_load
    else
      step "Removing '${DB_CONTAINER}' and starting over"
      docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true
      db_start
      db_load
    fi
    say ""
    say "Database reset."
    ;;

  *)
    fail "Usage: $0 [start|load|status|shell|stop|reset]"
    ;;
esac
