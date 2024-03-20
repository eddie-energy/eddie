ALTER TABLE dk_energinet.energinet_customer_permission_request
    ALTER COLUMN last_polled TYPE date,
    ALTER COLUMN start_timestamp TYPE date,
    ALTER COLUMN end_timestamp TYPE date;

ALTER TABLE dk_energinet.energinet_customer_permission_request
    RENAME COLUMN start_timestamp TO start_date;

ALTER TABLE dk_energinet.energinet_customer_permission_request
    RENAME COLUMN end_timestamp TO end_date;
