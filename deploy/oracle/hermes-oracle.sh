#!/bin/bash
# Creates or upgrades Hermes' Oracle (23ai or later) schemas; safe to run
# any number of times. Each Hermes database is a schema (user) named
# <prefix><database>, e.g. hermes_ebms, all with HERMES_DB_PASSWORD.
# Used as a /container-entrypoint-initdb.d script of gvenzl/oracle-free
# (first start) and by the db-ora-upgrade service (every start).
#
#   ORACLE_PASSWORD      SYS password
#   ORACLE_CONNECT       where the PDB is (default: localhost/FREEPDB1)
#   HERMES_DB_PASSWORD   the schemas' password
#   HERMES_ORACLE_SCHEMA_PREFIX  (default hermes_)
#   /sql                 h2o-installer/sql (read-only)
set -euo pipefail
: "${ORACLE_PASSWORD:?}" "${HERMES_DB_PASSWORD:?}"
connect=${ORACLE_CONNECT:-localhost/FREEPDB1}
prefix=${HERMES_ORACLE_SCHEMA_PREFIX:-hermes_}
password=${HERMES_DB_PASSWORD//\"/}

sql() { # $1 = user/password@connect [as sysdba]; SQL on stdin
    { echo "WHENEVER SQLERROR EXIT FAILURE"; echo "SET FEEDBACK OFF"; cat; echo "EXIT"; } \
        | sqlplus -S -L "$@" >/tmp/hermes-oracle.out || { cat /tmp/hermes-oracle.out >&2; return 1; }
}

for pair in ebms:oracle_ebms.sql as2:oracle_as2.sql as2plus:oracle_as2plus.sql sfrm:oracle_sfrm.sql \
            apikeys:oracle_apikeys.sql listenerports:oracle_listenerports.sql; do
    schema=$prefix${pair%%:*} script=${pair#*:}
    sql "sys/$ORACLE_PASSWORD@//$connect" as sysdba <<SQL
CREATE USER IF NOT EXISTS $schema IDENTIFIED BY "$password" QUOTA UNLIMITED ON USERS;
ALTER USER $schema IDENTIFIED BY "$password";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO $schema;
SQL
    # every CREATE made idempotent
    sed -E -e 's/CREATE TABLE( IF NOT EXISTS)? /CREATE TABLE IF NOT EXISTS /I' \
           -e 's/CREATE SEQUENCE( IF NOT EXISTS)? /CREATE SEQUENCE IF NOT EXISTS /I' "/sql/$script" \
        | sql "$schema/\"$password\"@//$connect"
done
sed "s/hermes_/$prefix/g" /sql/upgrade/oracle_upgrade.sql | sql "sys/$ORACLE_PASSWORD@//$connect" as sysdba
echo "hermes-oracle: schemas are up to date"
