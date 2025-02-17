CREATE TABLE IF NOT EXISTS aiida_application_information
(
    aiida_id   uuid PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);