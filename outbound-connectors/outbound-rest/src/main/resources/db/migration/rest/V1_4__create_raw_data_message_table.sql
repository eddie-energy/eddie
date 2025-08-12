CREATE TABLE rest.raw_data_message
(
    id          serial                   NOT NULL,
    inserted_at timestamp with time zone NOT NULL DEFAULT NOW(),
    payload     jsonb                    NOT NULL,
    PRIMARY KEY (id)
);