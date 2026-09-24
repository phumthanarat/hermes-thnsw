#!/bin/bash
# Brings the Hermes databases up to the current schema; safe to run any
# number of times. docker-compose runs it (service db-upgrade) before the
# application starts, so an existing database picks up new tables and
# columns on the next "docker compose up".
#
#   DB_HOST             MySQL host (default: db)
#   MYSQL_ROOT_PASSWORD root password
set -eu

mysql=(mysql -h"${DB_HOST:-db}" -uroot -p"$MYSQL_ROOT_PASSWORD")
for db in ebms as2 as2plus sfrm apikeys listenerports; do
    "${mysql[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$db\`"
    # every CREATE TABLE made idempotent
    sed -E 's/CREATE TABLE( IF NOT EXISTS)? /CREATE TABLE IF NOT EXISTS /' "/build/$db.sql" \
        | "${mysql[@]}" "$db"
done
if [ -n "${HERMES_DB_USER:-}" ]; then
    for db in ebms as2 as2plus sfrm apikeys listenerports; do
        "${mysql[@]}" -e "GRANT ALL ON \`$db\`.* TO '${HERMES_DB_USER}'@'%'" 2>/dev/null || true
    done
fi
"${mysql[@]}" < /build/upgrade.sql
echo "hermes-upgrade: schema is up to date"
