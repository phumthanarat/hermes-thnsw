#!/bin/bash
# One-off move of the Hermes data from the old MySQL 5.7 volume into the
# MySQL 8.4 database of the current stack. MySQL 8.4 cannot open a 5.7 data
# directory, so the old volume is read by a temporary 5.7 container and
# dumped. The old volume is left untouched (keep it until you're happy).
#
#   tools/migrate-from-mysql57.sh <old volume> <db service> [old root password]
#   e.g. tools/migrate-from-mysql57.sh deploy_h2o_db_data db h2o_db_password
set -euo pipefail
cd "$(dirname "$0")/.."

old_volume=${1:?usage: migrate-from-mysql57.sh <old volume> <db service> [old root password]}
db=${2:?db service (db or db2)}
old_root=${3:-h2o_db_password}
dbs="ebms as2 as2plus sfrm apikeys listenerports"
dump=$(mktemp --suffix=.sql)
temp=hermes-mysql57-export

docker rm -f "$temp" >/dev/null 2>&1 || true
docker run -d --name "$temp" -v "$old_volume":/var/lib/mysql mysql:5.7 >/dev/null
for i in $(seq 1 60); do
    docker exec "$temp" mysqladmin -uroot -p"$old_root" ping >/dev/null 2>&1 && break
    sleep 2
done
echo "dumping $dbs from $old_volume"
docker exec "$temp" mysqldump -uroot -p"$old_root" --single-transaction --routines \
    --databases $dbs > "$dump" 2>/dev/null
docker rm -f "$temp" >/dev/null
[ -s "$dump" ] || { echo "the dump is empty" >&2; exit 1; }

echo "loading into $db"
docker compose exec -T "$db" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < "$dump"
# pick up anything the 5.7 schema lacked
docker compose run --rm "$db-upgrade" >/dev/null
rm -f "$dump"
echo "done: restart the application (docker compose restart $([ "$db" = db2 ] && echo app2 || echo app))"
