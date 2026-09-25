-- Brings an existing Hermes PostgreSQL installation up to the current
-- schema. Safe to run any number of times, with psql as a superuser or the
-- tables' owner:  psql -f postgres_upgrade.sql
-- Missing tables: run each <db>.sql again with CREATE TABLE/SEQUENCE made
-- "IF NOT EXISTS" (deploy/postgres/hermes-upgrade.sh does that); this file
-- adds the columns added to tables that already existed.

\connect ebms
ALTER TABLE message ADD COLUMN IF NOT EXISTS created_via varchar(20);

\connect as2plus
ALTER TABLE message ADD COLUMN IF NOT EXISTS created_via varchar(20);
