CREATE TABLE cpa_party (
	cpa_id varchar(200) NOT NULL,
	from_party_id varchar(500),
	from_party_type varchar(500),
	to_party_id varchar(500),
	to_party_type varchar(500),
	source varchar(20),
	PRIMARY KEY (cpa_id)
);
