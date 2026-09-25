#!/bin/bash
# Creates or upgrades Hermes' PostgreSQL databases; safe to run any number
# of times. Used as /docker-entrypoint-initdb.d script of the postgres
# image (first start) and by the db-pg-upgrade service (every start).
#
#   HERMES_DB_USER / HERMES_DB_PASSWORD   the account Hermes connects with
#   PGHOST / PGUSER / PGPASSWORD          a superuser connection (upgrade)
#   /sql                                  h2o-installer/sql (read-only)
set -euo pipefail
: "${HERMES_DB_USER:?}" "${HERMES_DB_PASSWORD:?}"
psql=(psql -v ON_ERROR_STOP=1 -q --username "${PGUSER:-${POSTGRES_USER:-postgres}}")

role_exists=$("${psql[@]}" -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname = '$HERMES_DB_USER'")
password=${HERMES_DB_PASSWORD//\'/\'\'}
if [ -z "$role_exists" ]; then
    "${psql[@]}" -d postgres -c "CREATE ROLE \"$HERMES_DB_USER\" LOGIN PASSWORD '$password'"
fi

# database -> script
for pair in ebms:ebms.sql as2:as2.sql as2plus:as2plus.sql sfrm:sfrm.sql \
            apikeys:apikeys.sql listenerports:listenerports.sql; do
    db=${pair%%:*} script=${pair#*:}
    if [ -z "$("${psql[@]}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$db'")" ]; then
        "${psql[@]}" -d postgres -c "CREATE DATABASE \"$db\" OWNER \"$HERMES_DB_USER\""
    fi
    # objects owned by the Hermes account; every CREATE made idempotent
    { echo "SET ROLE \"$HERMES_DB_USER\";"
      sed -E -e 's/CREATE TABLE( IF NOT EXISTS)? /CREATE TABLE IF NOT EXISTS /I' \
             -e 's/CREATE SEQUENCE( IF NOT EXISTS)? /CREATE SEQUENCE IF NOT EXISTS /I' "/sql/$script"
    } | "${psql[@]}" -d "$db"
done
"${psql[@]}" -d postgres -c "SET ROLE \"$HERMES_DB_USER\"" -f /sql/upgrade/postgres_upgrade.sql >/dev/null
echo "hermes-postgres: databases are up to date"
