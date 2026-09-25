-- PostgreSQL: REST API keys
CREATE TABLE IF NOT EXISTS api_key (
	api_key varchar(64) NOT NULL,
	client_name varchar(255) NOT NULL,
	enabled varchar(5) NOT NULL DEFAULT 'true',
	created_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (api_key)
);
