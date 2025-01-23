CREATE SCHEMA IF NOT EXISTS core;

CREATE TABLE IF NOT EXISTS core.eddie_application_information
(
    eddie_id uuid PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);