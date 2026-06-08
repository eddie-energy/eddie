--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE mqtt_connection
(
    id                    bigserial PRIMARY KEY,
    source_data_source_id uuid,
    internal_host         VARCHAR(255) NOT NULL,
    external_host         VARCHAR(255) NOT NULL,
    mqtt_user_id          bigint REFERENCES data_source_mqtt_user (id)
);

ALTER TABLE data_source_mqtt
    ADD COLUMN mqtt_connection_id bigint;

INSERT INTO mqtt_connection (source_data_source_id,
                             internal_host,
                             external_host,
                             mqtt_user_id)
SELECT id,
       internal_host,
       external_host,
       mqtt_user_id
FROM data_source_mqtt;

UPDATE data_source_mqtt d
SET mqtt_connection_id = c.id FROM mqtt_connection C
WHERE C.source_data_source_id = d.id;

ALTER TABLE data_source_mqtt
    ADD CONSTRAINT data_source_mqtt_mqtt_connection_id_fkey
        FOREIGN KEY (mqtt_connection_id)
            REFERENCES mqtt_connection (id);

ALTER TABLE data_source_mqtt
DROP
CONSTRAINT data_source_mqtt_mqtt_user_id_fkey;

ALTER TABLE data_source_mqtt
DROP
COLUMN external_host,
    DROP
COLUMN internal_host,
    DROP
COLUMN mqtt_user_id;

ALTER TABLE mqtt_connection
DROP
COLUMN source_data_source_id;
