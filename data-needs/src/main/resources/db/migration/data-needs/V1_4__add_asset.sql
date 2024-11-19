ALTER TABLE generic_aiida_data_need
    ADD asset TEXT NOT NULL DEFAULT 'Connection Agreement Point';

ALTER TABLE smart_meter_aiida_data_need
    ADD asset TEXT NOT NULL DEFAULT 'Connection Agreement Point';