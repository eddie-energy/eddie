--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE data_source_mqtt_inbound_provisioning
(
    id                 bigserial PRIMARY KEY,
    mqtt_connection_id bigint NOT NULL UNIQUE REFERENCES mqtt_connection (id),
    mqtt_acl_id        bigint NOT NULL UNIQUE REFERENCES data_source_mqtt_acl (id)
);

-- Add columns for the current provisioning type and its optional MQTT configuration
ALTER TABLE data_source_mqtt_inbound
    ADD COLUMN provisioning_type text NOT NULL DEFAULT 'REST_BEARER',
    ADD COLUMN mqtt_provisioning_config_id bigint UNIQUE
        REFERENCES data_source_mqtt_inbound_provisioning (id);
