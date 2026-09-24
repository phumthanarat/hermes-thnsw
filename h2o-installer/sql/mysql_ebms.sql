CREATE TABLE IF NOT EXISTS message (
	message_id varchar(255),
	message_box varchar(255),
	message_type varchar(255),
	from_party_id varchar(255),
	from_party_role varchar(255),
	to_party_id varchar(255),
	to_party_role varchar(255),
	cpa_id varchar(255),
	service varchar(255),
	action varchar(255),
	conv_id varchar(255),
	ref_to_message_id varchar(255),-- message_id of the message that the response replies to 
	primal_message_id varchar(255),-- message_id of message which triggered "Resend as New Message"
	has_resend_as_new varchar(5),
	partnership_id varchar(255),
	sync_reply varchar(5),
	dup_elimination varchar(5),
	ack_requested varchar(5),
	ack_sign_requested varchar(5),
	sequence_no integer,
	sequence_status integer,
	sequence_group integer,
	time_to_live timestamp null default null,
	time_stamp timestamp null default null,
	timeout_time_stamp timestamp null default null,
	status varchar(2),
	status_description varchar(4000),
	created_via varchar(20),
	PRIMARY KEY (message_id, message_box)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS repository (
	message_id varchar(255),
	content_type varchar(255),
	content LONGBLOB,
	time_stamp timestamp null default null,
	message_box varchar(255),
	PRIMARY KEY (message_id, message_box)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS outbox (
	message_id varchar(255),
	retried integer,
	PRIMARY KEY (message_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS inbox (
	message_id varchar(255),
	order_no bigint,
	PRIMARY KEY (message_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS partnership (
	partnership_id varchar(255),
	cpa_id varchar(255),
	service varchar(255),
	action varchar(255),
	transport_protocol varchar(255),
	transport_endpoint varchar(1000),
	is_hostname_verified varchar(5),
	sync_reply_mode varchar(20),
	ack_requested varchar(20),
	ack_sign_requested varchar(20),
	dup_elimination varchar(20),
	actor varchar(255),
	disabled varchar(5),
	retries integer,
	retry_interval integer,
	persist_duration varchar(255),
	message_order varchar(13),
	sign_requested varchar(5),
	sign_cert LONGBLOB,
	ds_algorithm varchar(255),
	md_algorithm varchar(255),
	encrypt_requested varchar(5),
	encrypt_cert LONGBLOB,
	encrypt_algorithm varchar(5),
	PRIMARY KEY (partnership_id)
)ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS document_reference (
	reference_id int NOT NULL AUTO_INCREMENT,
	cpa_id varchar(50) NOT NULL,
	service varchar(200) NOT NULL,
	action varchar(200) NOT NULL,
	filename varchar(255) NOT NULL,
	file_type varchar(10) NOT NULL,
	content LONGBLOB NOT NULL,
	description varchar(500),
	disabled boolean NOT NULL DEFAULT false,
	uploaded_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (reference_id)
)ENGINE=INNODB;

-- party IDs of each CPA (from the CPA file on upload, or entered by an admin)
CREATE TABLE IF NOT EXISTS cpa_party (
	cpa_id varchar(200) NOT NULL,
	from_party_id varchar(500),
	from_party_type varchar(500),
	to_party_id varchar(500),
	to_party_type varchar(500),
	source varchar(20),
	PRIMARY KEY (cpa_id)
)ENGINE=INNODB;
