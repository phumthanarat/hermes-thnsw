#!/bin/bash
# Restores a backup made by the backup service into a running stack.
#
#   deploy/tools/restore.sh deploy/backups/hermes-YYYYmmdd-HHMMSS [app|app2]
#
# Replaces the databases and the application's repository volume with the
# backup's, then restarts the application. Stop anything sending messages
# first: what arrived after the backup is lost.
set -euo pipefail
cd "$(dirname "$0")/.."

backup=${1:?usage: restore.sh <backup directory> [app|app2]}
app=${2:-app}
db=$([ "$app" = app2 ] && echo db2 || echo db)
[ -f "$backup/databases.sql.gz" ] && [ -f "$backup/repository.tar.gz" ] \
    || { echo "not a backup directory: $backup" >&2; exit 1; }

read -r -p "Replace the $db databases and the $app repository with $backup? [y/N] " answer
[ "$answer" = y ] || exit 1

docker compose stop "$app"
gunzip -c "$backup/databases.sql.gz" \
    | docker compose exec -T "$db" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
docker compose run --rm --no-deps -v "$(realpath "$backup"):/restore:ro" --entrypoint sh "$app" \
    -c 'rm -rf /hermes_home/repository/* /hermes_home/repository/.[!.]* && tar -xzf /restore/repository.tar.gz -C /hermes_home/repository'
docker compose start "$app"
echo "restored $backup into $db / $app"
