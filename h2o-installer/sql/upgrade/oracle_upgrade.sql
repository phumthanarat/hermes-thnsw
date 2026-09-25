-- Brings an existing Hermes Oracle (23ai or later) installation up to the
-- current schema. Safe to run any number of times, as a DBA:
--   sql / as sysdba @oracle_upgrade.sql
-- The schemas are named hermes_<database> (see HERMES_ORACLE_SCHEMA_PREFIX
-- in deploy/app_server/hermes-config.sh); adjust the names if yours differ.
-- Missing tables: run each oracle_<db>.sql again in its schema (they use
-- CREATE TABLE IF NOT EXISTS); this file adds the columns added to tables
-- that already existed.

-- a column that already exists (ORA-01430) is left as it is
DECLARE
    PROCEDURE add_column(ddl VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE ddl;
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
    END;
BEGIN
    add_column('ALTER TABLE hermes_ebms.message ADD (created_via varchar2(20))');
    add_column('ALTER TABLE hermes_as2plus.message ADD (created_via varchar2(20))');
END;
/
