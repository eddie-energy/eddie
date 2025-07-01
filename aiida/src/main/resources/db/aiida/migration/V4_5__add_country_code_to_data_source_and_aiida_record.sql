ALTER TABLE data_source
    ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);

UPDATE data_source
SET country_code = CASE data_source_type
                       WHEN 'SMART_METER_ADAPTER' THEN 'AT'
                       WHEN 'MICRO_TELEINFO' THEN 'FR'
                       WHEN 'SMART_GATEWAYS_ADAPTER' THEN 'NL'
                       WHEN 'SIMULATION' THEN 'AT'
                       WHEN 'MODBUS' THEN 'AT'
    END;

ALTER TABLE data_source
    ALTER COLUMN country_code SET NOT NULL;