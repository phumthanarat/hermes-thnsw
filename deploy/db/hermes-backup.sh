#!/bin/bash
# Daily backup (docker-compose service "backup"): dumps the Hermes databases
# and archives the application's repository volume (message content,
# console users, HTTPS certificate), then deletes backups older than
# BACKUP_KEEP_DAYS.
#
#   DB_HOST             MySQL host (default: db)
#   MYSQL_ROOT_PASSWORD root password
#   BACKUP_TIME         daily time, HH:MM server time (default 01:30)
#   BACKUP_KEEP_DAYS    days to keep (default 14)
#   BACKUP_NAME         file name prefix (default hermes)
#   BACKUP_ON_START     "true" to also back up right away
#   BACKUP_UID/GID      owner of the backup files (the host user)
#   /backups            where backups are written
#   /app-repository     the application's repository volume (read-only)
#
# "hermes-backup.sh now" makes one backup and exits.
set -u

# backups hold every message and secret: readable by their owner only
umask 077

DBS="ebms as2 as2plus sfrm apikeys listenerports"
name=${BACKUP_NAME:-hermes}

backup() {
    stamp=$(date +%Y%m%d-%H%M%S)
    dir=/backups/$name-$stamp
    mkdir -p "$dir.partial"
    if mysqldump -h"${DB_HOST:-db}" -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction \
            --routines --databases $DBS 2>"$dir.partial/mysqldump.err" | gzip > "$dir.partial/databases.sql.gz" \
        && [ "${PIPESTATUS[0]}" -eq 0 ] \
        && tar -czf "$dir.partial/repository.tar.gz" -C /app-repository . ; then
        rm -f "$dir.partial/mysqldump.err"
        mv "$dir.partial" "$dir"
        # owned by the host user who runs the stack (setup.sh), so restore.sh
        # can read them; nobody else can
        [ -n "${BACKUP_UID:-}" ] && chown -R "$BACKUP_UID:${BACKUP_GID:-$BACKUP_UID}" "$dir"
        echo "hermes-backup: $dir ($(du -sh "$dir" | cut -f1))"
    else
        echo "hermes-backup: FAILED, see $dir.partial" >&2
        return 1
    fi
    find /backups -maxdepth 1 -name "$name-*" -type d -mtime +"${BACKUP_KEEP_DAYS:-14}" \
        -exec rm -rf {} + -exec echo "hermes-backup: removed old {}" \;
}

if [ "${1:-}" = "now" ]; then
    backup
    exit $?
fi

[ "${BACKUP_ON_START:-false}" = "true" ] && backup
while true; do
    now=$(date +%s)
    next=$(date -d "today ${BACKUP_TIME:-01:30}" +%s)
    [ "$next" -le "$now" ] && next=$(date -d "tomorrow ${BACKUP_TIME:-01:30}" +%s)
    sleep $((next - now))
    backup
done
