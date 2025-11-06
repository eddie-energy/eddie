CREATE TABLE rest.validated_historical_data_marked_document_v1_04
(
    id          serial                   NOT NULL,
    inserted_at timestamp with time zone NOT NULL DEFAULT NOW(),
    payload     jsonb                    NOT NULL,
    PRIMARY KEY (id)
);