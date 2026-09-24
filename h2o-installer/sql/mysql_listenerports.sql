CREATE TABLE IF NOT EXISTS listener_port (
	port_id varchar(50) NOT NULL,
	name varchar(200) NOT NULL,
	bind_address varchar(100) NOT NULL DEFAULT '0.0.0.0',
	port int NOT NULL,
	use_tls boolean NOT NULL DEFAULT false,
	target_service varchar(20) NOT NULL,
	is_disabled boolean NOT NULL DEFAULT false,
	description varchar(500),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (port_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS ca_issued_cert (
	serial_number varchar(50) NOT NULL,
	subject_dn varchar(500) NOT NULL,
	cert blob NOT NULL,
	revoked boolean NOT NULL DEFAULT false,
	issued_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	revoked_timestamp timestamp NULL DEFAULT NULL,
	PRIMARY KEY (serial_number)
)ENGINE=INNODB;

-- admin console: who did what (Access > Audit Log)
CREATE TABLE IF NOT EXISTS audit_log (
	audit_id bigint NOT NULL AUTO_INCREMENT,
	event_time timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
	username varchar(64),
	client_ip varchar(64),
	action varchar(100) NOT NULL,
	target varchar(500),
	outcome varchar(20) NOT NULL,
	detail varchar(1000),
	PRIMARY KEY (audit_id),
	KEY audit_log_time (event_time),
	KEY audit_log_user (username)
)ENGINE=INNODB;

-- admin console: per-user password age/history and two-factor secret
CREATE TABLE IF NOT EXISTS console_user_security (
	username varchar(64) NOT NULL,
	password_changed timestamp NULL DEFAULT NULL,
	password_history varchar(4000),
	totp_secret varchar(64),
	totp_enabled varchar(5) NOT NULL DEFAULT 'false',
	totp_last_step bigint,
	PRIMARY KEY (username)
)ENGINE=INNODB;

-- admin console: security settings (Access > Security Settings)
CREATE TABLE IF NOT EXISTS console_setting (
	name varchar(64) NOT NULL,
	value varchar(500),
	PRIMARY KEY (name)
)ENGINE=INNODB;
