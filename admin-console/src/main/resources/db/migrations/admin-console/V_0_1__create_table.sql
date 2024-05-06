-- Set the schema as the current one
SET search_path TO admin_console;

-- Create the 'status_messages' table within the schema
CREATE TABLE IF NOT EXISTS status_messages (
    id SERIAL PRIMARY KEY,
    permission_id VARCHAR(255) NOT NULL,
    timestamps VARCHAR(255),
    status VARCHAR(255)
);