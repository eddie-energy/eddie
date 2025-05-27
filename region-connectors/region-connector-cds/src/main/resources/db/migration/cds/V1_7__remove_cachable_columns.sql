DROP TABLE IF EXISTS cds.coverage;
ALTER TABLE cds.cds_server
    DROP COLUMN name,
    DROP COLUMN pushed_authorization_request_endpoint,
    DROP COLUMN authorization_endpoint,
    DROP COLUMN token_endpoint,
    DROP COLUMN clients_endpoint,
    DROP COLUMN credentials_endpoint,
    DROP COLUMN accounts_endpoint,
    DROP COLUMN service_contracts_endpoint,
    DROP COLUMN service_points_endpoint,
    DROP COLUMN meter_device_endpoint,
    DROP COLUMN bill_section_endpoint,
    DROP COLUMN usage_point_endpoint,
    DROP COLUMN customer_data_client_id,
    DROP COLUMN customer_data_client_secret;
