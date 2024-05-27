-- Remove not needed tables/columns/constraints
DROP TABLE permission_requested_codes;

ALTER TABLE permission
    DROP COLUMN data_need_id;

ALTER TABLE permission
    DROP CONSTRAINT permission_status_check;

ALTER TABLE permission
    ALTER COLUMN connection_id DROP NOT NULL,
    ALTER COLUMN expiration_time DROP NOT NULL,
    ALTER COLUMN grant_time DROP NOT NULL,
    ALTER COLUMN start_time DROP NOT NULL,
    ALTER COLUMN permission_id TYPE VARCHAR(36);


-- Create new tables/columns
CREATE TABLE aiida_local_data_need
(
    permission_id         VARCHAR(36) NOT NULL PRIMARY KEY REFERENCES permission (permission_id),
    data_need_id          VARCHAR(36) NOT NULL,
    type                  TEXT        NOT NULL,
    name                  TEXT        NOT NULL,
    purpose               TEXT        NOT NULL,
    policy_link           TEXT        NOT NULL,
    transmission_interval INTEGER     NOT NULL
);

CREATE TABLE aiida_local_data_need_data_tags
(
    data_need_id VARCHAR(36) NOT NULL,
    data_tags    TEXT        NOT NULL
);

ALTER TABLE permission
    ADD access_token                        TEXT NOT NULL DEFAULT 'migration default value',
    ADD handshake_url                       TEXT NOT NULL DEFAULT 'migration default value',
    ADD data_need_permission_id             VARCHAR(36),
    ADD mqtt_streaming_config_permission_id VARCHAR(36);


-- Cannot further process old permissions as they lack required MQTT and data need information
UPDATE permission
SET status       = 'UNFULFILLABLE',
    access_token = 'Cannot fulfill this permission because it was created with an older version of AIIDA and the permission cannot be migrated to the current AIIDA version.'
WHERE status IN ('ACCEPTED', 'WAITING_FOR_START', 'STREAMING_DATA', 'REVOCATION_RECEIVED');
