CREATE TABLE IF NOT EXISTS sfrm_ftp_channel (
	channel_id varchar(50) NOT NULL,
	name varchar(200) NOT NULL,
	host varchar(255) NOT NULL,
	port int NOT NULL DEFAULT 21,
	username varchar(200),
	password_encrypted varchar(1000),
	remote_path varchar(500) NOT NULL DEFAULT '/',
	is_passive_mode boolean NOT NULL DEFAULT true,
	use_tls boolean NOT NULL DEFAULT false,
	tls_cert_fingerprint varchar(200),
	polling_interval int,
	max_files_per_poll int,
	is_disabled boolean NOT NULL DEFAULT false,
	description varchar(500),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (channel_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS sfrm_sftp_channel (
	channel_id varchar(50) NOT NULL,
	name varchar(200) NOT NULL,
	host varchar(255) NOT NULL,
	port int NOT NULL DEFAULT 22,
	username varchar(200),
	password_encrypted varchar(1000),
	remote_path varchar(500) NOT NULL DEFAULT '/',
	host_key_fingerprint varchar(200),
	polling_interval int,
	max_files_per_poll int,
	is_disabled boolean NOT NULL DEFAULT false,
	description varchar(500),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (channel_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS sfrm_file_polling_channel (
	channel_id varchar(50) NOT NULL,
	name varchar(200) NOT NULL,
	watch_path varchar(500) NOT NULL,
	polling_interval int,
	max_files_per_poll int,
	is_disabled boolean NOT NULL DEFAULT false,
	description varchar(500),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (channel_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS sfrm_mail_channel (
	channel_id varchar(50) NOT NULL,
	name varchar(200) NOT NULL,
	protocol varchar(10) NOT NULL DEFAULT 'imap',
	host varchar(255) NOT NULL,
	port int,
	username varchar(200),
	password_encrypted varchar(1000),
	folder varchar(200) NOT NULL DEFAULT 'INBOX',
	use_ssl boolean NOT NULL DEFAULT true,
	polling_interval int,
	max_messages_per_poll int,
	is_disabled boolean NOT NULL DEFAULT false,
	description varchar(500),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (channel_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS sfrm_http_channel (
	channel_id varchar(50) NOT NULL,
	name varchar(200) NOT NULL,
	bind_address varchar(100) NOT NULL DEFAULT '0.0.0.0',
	port int NOT NULL,
	use_tls boolean NOT NULL DEFAULT false,
	target_service varchar(20) NOT NULL,
	is_disabled boolean NOT NULL DEFAULT false,
	description varchar(500),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (channel_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS sfrm_partnership (
	partnership_seq int NOT NULL AUTO_INCREMENT,
	partnership_id varchar(50) NOT NULL,
	description varchar(200),
	partner_endpoint varchar(200) NOT NULL,
	partner_cert_fingerprint varchar(200),
	is_hostname_verified boolean DEFAULT false,
	sign_algorithm varchar(200),
	encrypt_algorithm varchar(200),
	retry_max int DEFAULT 3,
	retry_interval int DEFAULT 30000,
	is_disabled boolean NOT NULL DEFAULT false,
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (partnership_seq),
	UNIQUE (partnership_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS sfrm_message (
	message_id varchar(200) NOT NULL,
	message_box varchar(200) NOT NULL,
	partnership_id varchar(50) NOT NULL,
	partner_endpoint varchar(200) NOT NULL,
	total_segment int,
	total_size bigint,
	is_hostname_verified boolean,
	partner_cert_content varchar(200),
	sign_algorithm varchar(200),
	encrypt_algorithm varchar(200),
	status varchar(200) NOT NULL,
	status_desc varchar(200),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	proceed_timestamp timestamp NULL,
	completed_timestamp timestamp NULL,
	filename varchar(200),
	PRIMARY KEY (message_id, message_box)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS sfrm_message_segment (
	message_id varchar(200) NOT NULL,
	message_box varchar(200) NOT NULL,
	segment_no int NOT NULL,
	segment_type varchar(200) NOT NULL,
	segment_start bigint,
	segment_end bigint,
	retried int DEFAULT -1,
	md5_value varchar(200),
	status varchar(200),
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	proceed_timestamp timestamp NULL,
	completed_timestamp timestamp NULL,
	PRIMARY KEY (message_id, message_box, segment_no, segment_type)
)ENGINE=INNODB;
