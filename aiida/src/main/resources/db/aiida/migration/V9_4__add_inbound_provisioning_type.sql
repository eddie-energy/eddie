--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Add column to identify the current provisioning type
ALTER TABLE data_source_mqtt_inbound
    ADD COLUMN provisioning_type text NOT NULL DEFAULT 'REST_BEARER',
    ADD COLUMN mqtt_connection_id bigint,
    ADD COLUMN mqtt_acl_id bigint;
