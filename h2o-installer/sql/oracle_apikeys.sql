-- Oracle Database 23ai or later: REST API keys
CREATE TABLE IF NOT EXISTS api_key (
	api_key varchar2(64) NOT NULL,
	client_name varchar2(255) NOT NULL,
	enabled varchar2(5) DEFAULT 'true' NOT NULL,
	created_timestamp timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	PRIMARY KEY (api_key)
);
