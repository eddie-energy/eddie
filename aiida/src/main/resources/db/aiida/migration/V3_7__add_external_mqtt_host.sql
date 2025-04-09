ALTER TABLE data_source RENAME COLUMN mqtt_server_uri TO mqtt_internal_host;

ALTER  TABLE data_source ADD COLUMN mqtt_external_host TEXT DEFAULT NULL;