CREATE TABLE rest.accounting_point_data_market_document
(
    id          serial                   NOT NULL,
    inserted_at timestamp with time zone NOT NULL DEFAULT NOW(),
    payload     jsonb                    NOT NULL,
    PRIMARY KEY (id)
);