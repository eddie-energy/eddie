-- Create the 'status_messages' table within the schema

DROP TABLE IF EXISTS status_messages;

CREATE TABLE status_messages (
    id SERIAL PRIMARY KEY,
    permission_id VARCHAR(255) NOT NULL,
    country VARCHAR(255),
    dso VARCHAR(255),
    start_date VARCHAR(255),
    status VARCHAR(255)
);