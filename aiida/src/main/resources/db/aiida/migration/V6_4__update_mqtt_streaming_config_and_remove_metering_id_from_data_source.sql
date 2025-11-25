ALTER TABLE mqtt_streaming_config
    DROP COLUMN username;

ALTER TABLE mqtt_streaming_config
    DROP CONSTRAINT mqtt_streaming_config_permission_id_fkey;

ALTER TABLE mqtt_streaming_config
    ADD CONSTRAINT mqtt_streaming_config_permission_id_fkey
        FOREIGN KEY (permission_id)
            REFERENCES permission (permission_id)
            ON DELETE CASCADE;

ALTER TABLE data_source DROP metering_id;