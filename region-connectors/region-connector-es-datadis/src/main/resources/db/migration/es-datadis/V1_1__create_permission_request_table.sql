ALTER TABLE es_datadis.datadis_permission_request
    DROP COLUMN IF EXISTS permissionstart;

ALTER TABLE es_datadis.datadis_permission_request
    DROP COLUMN IF EXISTS permissionend;

ALTER TABLE es_datadis.datadis_permission_request
    ALTER COLUMN request_data_from TYPE date,
    ALTER COLUMN request_data_to TYPE date;
