--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Create new table for mqtt connections
CREATE TABLE mqtt_connection
(
    id                    bigserial PRIMARY KEY,
    source_data_source_id uuid, -- temporary column for migration
    internal_host         VARCHAR(255) NOT NULL,
    external_host         VARCHAR(255) NOT NULL,
    mqtt_user_id          bigint UNIQUE REFERENCES data_source_mqtt_user (id)
);

-- Add column to map connections to data sources
ALTER TABLE data_source_mqtt
    ADD COLUMN mqtt_connection_id bigint;

-- Migrate existing rows
INSERT INTO mqtt_connection (source_data_source_id,
                             internal_host,
                             external_host,
                             mqtt_user_id)
SELECT id,
       internal_host,
       external_host,
       mqtt_user_id
FROM data_source_mqtt;

-- Link old rows to new connection rows
UPDATE data_source_mqtt d
SET mqtt_connection_id = c.id
FROM mqtt_connection c
WHERE c.source_data_source_id = d.id;

-- Enforce one-to-one mapping between data_source_mqtt and mqtt_connection
ALTER TABLE data_source_mqtt
    ALTER COLUMN mqtt_connection_id SET NOT NULL,
    ADD CONSTRAINT data_source_mqtt_mqtt_connection_id_key
        UNIQUE (mqtt_connection_id),
    ADD CONSTRAINT data_source_mqtt_mqtt_connection_id_fkey
        FOREIGN KEY (mqtt_connection_id)
            REFERENCES mqtt_connection (id);

-- Remove old foreign key
ALTER TABLE data_source_mqtt
    DROP CONSTRAINT data_source_mqtt_mqtt_user_id_fkey;

-- Drop columns moved to the new table
ALTER TABLE data_source_mqtt
    DROP COLUMN external_host,
    DROP COLUMN internal_host,
    DROP COLUMN mqtt_user_id;

-- Drop temporary mapping column
ALTER TABLE mqtt_connection
    DROP COLUMN source_data_source_id;