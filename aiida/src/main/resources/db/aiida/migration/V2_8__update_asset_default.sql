ALTER TABLE aiida_local_data_need
    DROP COLUMN asset;

ALTER TABLE aiida_local_data_need
    ADD asset TEXT NOT NULL DEFAULT 'CONNECTION_AGREEMENT_POINT';