CREATE SEQUENCE partnership_seq START 1; 

CREATE TABLE sfrm_partnership
(
  partnership_seq int DEFAULT nextval('partnership_seq'),
  partnership_id varchar(50) NOT NULL,
  description varchar,
  partner_endpoint varchar NOT NULL,
  partner_cert_fingerprint varchar,
  is_hostname_verified boolean default 'false',
  sign_algorithm varchar,
  encrypt_algorithm varchar,
  retry_max int default 3,
  retry_interval int default 30000,
  is_disabled boolean NOT NULL default 'false',  
  created_timestamp timestamp NOT NULL DEFAULT now(),
  modified_timestamp timestamp NOT NULL DEFAULT now(),
  UNIQUE (partnership_id),
  PRIMARY KEY (partnership_seq)
);

CREATE TABLE sfrm_message
(
  message_id varchar NOT NULL,
  message_box varchar NOT NULL,
  partnership_id varchar NOT NULL, 
  partner_endpoint varchar NOT NULL,
  total_segment int,
  total_size bigint,
  is_hostname_verified boolean,
  partner_cert_content varchar,
  sign_algorithm varchar,
  encrypt_algorithm varchar,
  status varchar NOT NULL,
  status_desc varchar,
  created_timestamp timestamp NOT NULL DEFAULT now(),
  proceed_timestamp timestamp,
  completed_timestamp timestamp,
  filename varchar,
  PRIMARY KEY (message_id, message_box)
);

CREATE TABLE sfrm_message_segment 
(
   message_id varchar NOT NULL,
   message_box varchar NOT NULL,
   segment_no int NOT NULL,
   segment_type varchar NOT NULL,
   segment_start bigint,
   segment_end bigint,
   retried int DEFAULT -1,
   md5_value varchar,
   status varchar,
   created_timestamp timestamp NOT NULL DEFAULT now(),
   proceed_timestamp timestamp,
   completed_timestamp timestamp,
   PRIMARY KEY (message_id, message_box, segment_no, segment_type)
);

-- polling channels (FTP, SFTP, folder, mail)
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
);

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
);

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
);

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
);
