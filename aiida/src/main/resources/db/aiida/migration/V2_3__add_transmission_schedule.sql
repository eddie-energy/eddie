ALTER TABLE aiida_local_data_need
    DROP COLUMN transmission_interval;

ALTER TABLE aiida_local_data_need
    ADD transmission_schedule VARCHAR(36) NOT NULL DEFAULT '* * * * * *';
