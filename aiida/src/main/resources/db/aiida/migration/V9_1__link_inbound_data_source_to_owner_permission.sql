--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE data_source_mqtt_inbound
    ADD COLUMN permission_id uuid;

UPDATE data_source_mqtt_inbound inbound
SET permission_id = p.permission_id
FROM permission p
WHERE p.data_source_id = inbound.id;

ALTER TABLE data_source_mqtt_inbound
    ADD CONSTRAINT fk_data_source_mqtt_inbound_permission
        FOREIGN KEY (permission_id) REFERENCES permission (permission_id);

ALTER TABLE data_source_mqtt_inbound
    ADD CONSTRAINT uk_data_source_mqtt_inbound_permission UNIQUE (permission_id);

ALTER TABLE permission
    DROP CONSTRAINT fk_permission_to_data_source,
    ADD CONSTRAINT fk_permission_to_data_source
        FOREIGN KEY (data_source_id) REFERENCES data_source (id) ON DELETE RESTRICT;
