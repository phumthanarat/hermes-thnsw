-- Brings an existing Hermes MySQL installation up to the current schema.
-- Safe to run any number of times. Missing tables are created by running
-- each mysql_<db>.sql with CREATE TABLE IF NOT EXISTS (see
-- deploy/db/hermes-upgrade.sh); this file adds the columns added to tables
-- that already existed.

-- the helper procedure lives in ebms for the duration of the script
USE ebms;
DROP PROCEDURE IF EXISTS hermes_add_column;
DELIMITER //
CREATE PROCEDURE hermes_add_column(IN db VARCHAR(64), IN tbl VARCHAR(64),
                                   IN col VARCHAR(64), IN definition VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = db AND table_name = tbl AND column_name = col) THEN
        SET @ddl = CONCAT('ALTER TABLE `', db, '`.`', tbl, '` ADD COLUMN `', col, '` ', definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- how a message was created ("webservice_api", "admin_console"), for the
-- Source badge in Message History
CALL hermes_add_column('ebms', 'message', 'created_via', 'varchar(20)');
CALL hermes_add_column('as2plus', 'message', 'created_via', 'varchar(20)');

DROP PROCEDURE hermes_add_column;
