-- Create the 'status_messages' table within the schema
CREATE TABLE status_messages
(
    id            SERIAL PRIMARY KEY,
    permission_id VARCHAR(255) NOT NULL,
    timestamp     VARCHAR(255),
    status        VARCHAR(255)
);
