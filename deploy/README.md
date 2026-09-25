# Running Hermes with Docker

Two gateways (`app`/`db` and `app2`/`db2`, e.g. for loopback tests) plus test
FTP/SFTP/mail servers.

| Component | Version |
|---|---|
| Application server | Apache Tomcat 9.0 on Java 21 (Ubuntu 24.04) |
| Database | MySQL 8.4 LTS |

## First start

```sh
cd deploy
./setup.sh            # writes .env: random database passwords, host name, backup time
docker compose up -d
```

Open `https://<host>:18443/corvus/admin/home` (app2: port 28443) and sign in as
**admin / admin**; you are asked to set a new password straight away.
Plain `http://` addresses redirect to HTTPS.

`.env` holds the passwords: keep it private (it is git-ignored).

## Database: MySQL, PostgreSQL or Oracle

The application image works with any of them; `HERMES_DB_TYPE` picks one
(`mysql`, the default; `postgres`; `oracle`).

| | MySQL (default) | PostgreSQL | Oracle |
|---|---|---|---|
| Version | 8.4 | 12 or later (tested 16) | 23ai or later (tested 23ai Free) |
| Stack | `docker-compose.yml` | `docker-compose.postgres.yml` (app-pg, ports 38080/38443) | `docker-compose.oracle.yml` (app-ora, ports 48080/48443) |
| Schema scripts | `h2o-installer/sql/mysql_*.sql` | `h2o-installer/sql/<db>.sql` | `h2o-installer/sql/oracle_*.sql` |
| Upgrade | `upgrade/mysql_upgrade.sql` | `upgrade/postgres_upgrade.sql` | `upgrade/oracle_upgrade.sql` |

```sh
docker compose -f docker-compose.postgres.yml up -d
docker compose -f docker-compose.oracle.yml up -d   # first start takes a few minutes
```

Every feature works on all three (ebMS, AS2, SFRM, Documents, Ping,
Housekeeping, API keys, listener ports, CA, console security). Oracle keeps
each Hermes database as a schema `hermes_<database>` (e.g. `hermes_ebms`,
prefix `HERMES_ORACLE_SCHEMA_PREFIX`) in the PDB `HERMES_DB_SERVICE`
(default `FREEPDB1`); it needs 23ai for its BOOLEAN type and
`CREATE ... IF NOT EXISTS`.

Settings for an external database (in `.env` or the compose file):
`HERMES_DB_TYPE`, `HERMES_DB_HOST`, `HERMES_DB_PORT`, `HERMES_DB_USER`
(MySQL/PostgreSQL), `HERMES_DB_PASSWORD`, `HERMES_DB_SERVICE` (Oracle).

## Database password

* Set on first start from `.env` (`HERMES_DB_PASSWORD`, `DB_ROOT_PASSWORD`).
* Change it later with `tools/change-db-password.sh` (random) or
  `tools/change-db-password.sh --prompt`: it changes the password in MySQL and
  in `.env`, then restarts the applications. Don't just edit `.env`: the
  database keeps the old one.

## HTTPS certificate

A self-signed certificate for `APP_TLS_HOSTNAME` is generated on first start.
Replace it with a real one from the admin console (**Main > HTTPS
Certificate**, no restart needed), or start with PEM files:

```sh
# docker-compose.override.yml
services:
  app:
    volumes: ["./tls:/tls:ro"]
    environment:
      APP_TLS_CERT_FILE: /tls/fullchain.pem
      APP_TLS_KEY_FILE: /tls/privkey.pem
```

The certificate is kept on the repository volume (`tls/server.p12`).

## Backups

The `backup` / `backup2` services back up every day at `BACKUP_TIME` into
`deploy/backups/hermes-app-<date>/` (databases + repository volume: message
content, console users, certificate) and delete those older than
`BACKUP_KEEP_DAYS`. Backups are readable by the host user only.

```sh
docker compose exec backup hermes-backup.sh now      # back up now
tools/restore.sh backups/hermes-app-20260924-013000  # restore app/db
tools/restore.sh backups/hermes-app2-20260924-013000 app2
```

## Upgrading

`docker compose build && docker compose up -d`. The `db-upgrade` services run
before the applications start and bring an existing database up to the
current schema (new tables and columns); it is safe to run any number of
times. For an installation outside Docker, run
`h2o-installer/sql/upgrade/mysql_upgrade.sql` after the `mysql_<db>.sql`
scripts (see `deploy/db/hermes-upgrade.sh`).

### From the MySQL 5.7 stack

MySQL 8.4 can't open a 5.7 data directory, so the data moves once:

```sh
docker compose up -d db db2
tools/migrate-from-mysql57.sh deploy_h2o_db_data db h2o_db_password
tools/migrate-from-mysql57.sh deploy_h2o_db2_data db2 h2o_db_password
docker compose up -d
```

The old volumes are left untouched; remove them with `docker volume rm` once
everything is checked.

## Console users

See **Access > Users** in the admin console. A forgotten administrator
password: start once with `APP_ADMIN_RESET=true docker compose up -d app`,
then `docker compose up -d app` again (while set, every start resets it).
