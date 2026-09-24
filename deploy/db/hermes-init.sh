#!/bin/bash
# Runs once, when the MySQL data volume is first initialised
# (/docker-entrypoint-initdb.d): creates Hermes' databases and its account.
#
#   HERMES_DB_USER / HERMES_DB_PASSWORD   the account Hermes connects with
#
# To change the password of an existing database, use
# deploy/tools/change-db-password.sh.
set -eu

: "${HERMES_DB_USER:?HERMES_DB_USER must be set}"
: "${HERMES_DB_PASSWORD:?HERMES_DB_PASSWORD must be set}"

mysql=(mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD")

"${mysql[@]}" <<SQL
CREATE USER IF NOT EXISTS '${HERMES_DB_USER}'@'%' IDENTIFIED BY '${HERMES_DB_PASSWORD//\'/\'\'}';
SQL

for db in ebms as2 as2plus sfrm apikeys listenerports; do
    "${mysql[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$db\`; GRANT ALL ON \`$db\`.* TO '${HERMES_DB_USER}'@'%';"
    "${mysql[@]}" "$db" < "/build/$db.sql"
done
