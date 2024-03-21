ALTER TABLE es_datadis.datadis_permission_request
    DROP COLUMN IF EXISTS permission_start;

ALTER TABLE es_datadis.datadis_permission_request
    DROP COLUMN IF EXISTS permission_end;

ALTER TABLE es_datadis.datadis_permission_request
    ALTER COLUMN request_data_from TYPE date,
    ALTER COLUMN request_data_to TYPE date;
