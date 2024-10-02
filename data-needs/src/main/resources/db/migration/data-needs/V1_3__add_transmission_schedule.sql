ALTER TABLE generic_aiida_data_need
    DROP COLUMN transmission_interval;

ALTER TABLE smart_meter_aiida_data_need
    DROP COLUMN transmission_interval;

ALTER TABLE generic_aiida_data_need
    ADD transmission_schedule VARCHAR(36) NOT NULL DEFAULT '* * * * * *';

ALTER TABLE smart_meter_aiida_data_need
    ADD transmission_schedule VARCHAR(36) NOT NULL DEFAULT '* * * * * *';