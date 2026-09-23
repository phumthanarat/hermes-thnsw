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
