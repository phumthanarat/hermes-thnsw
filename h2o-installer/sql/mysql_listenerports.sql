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
