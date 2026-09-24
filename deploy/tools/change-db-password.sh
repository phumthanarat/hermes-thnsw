#!/bin/bash
# Changes the password of the MySQL account Hermes uses (and optionally the
# root password), in the running databases and in deploy/.env, then
# restarts the applications so they use it.
#
#   tools/change-db-password.sh            new random passwords
#   tools/change-db-password.sh --prompt   type the new Hermes password
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { echo "no deploy/.env: run setup.sh first" >&2; exit 1; }
set -a; . ./.env; set +a

random() { head -c 32 /dev/urandom | base64 | tr -d '/+=\n' | cut -c1-24; }
if [ "${1:-}" = "--prompt" ]; then
    read -r -s -p "New password for $HERMES_DB_USER: " new; echo
    [ ${#new} -ge 12 ] || { echo "use at least 12 characters" >&2; exit 1; }
else
    new=$(random)
fi

sql_quote() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"; }
for db in db db2; do
    docker compose ps --status running --services | grep -qx "$db" || continue
    echo "ALTER USER $(sql_quote "$HERMES_DB_USER")@'%' IDENTIFIED BY $(sql_quote "$new");" \
        | docker compose exec -T "$db" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
    echo "changed the password on $db"
done
tmp=$(mktemp); umask 077
sed "s/^HERMES_DB_PASSWORD=.*/HERMES_DB_PASSWORD=$(printf '%s' "$new" | sed 's/[&/\\]/\\&/g')/" .env > "$tmp" && cat "$tmp" > .env && rm -f "$tmp"
docker compose up -d app app2
echo "deploy/.env updated and the applications restarted"
