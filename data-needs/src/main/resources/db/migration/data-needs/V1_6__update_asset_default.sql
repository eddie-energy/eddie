ALTER TABLE generic_aiida_data_need
    DROP COLUMN asset;

ALTER TABLE smart_meter_aiida_data_need
    DROP COLUMN asset;

ALTER TABLE generic_aiida_data_need
    ADD asset TEXT NOT NULL DEFAULT 'CONNECTION_AGREEMENT_POINT';

ALTER TABLE smart_meter_aiida_data_need
    ADD asset TEXT NOT NULL DEFAULT 'CONNECTION_AGREEMENT_POINT';