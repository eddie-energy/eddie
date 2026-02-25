--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE aiida_local_data_need
    ADD COLUMN is_acknowledgement_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE mqtt_streaming_config
    ADD COLUMN acknowledgement_topic TEXT NOT NULL DEFAULT '';

ALTER TABLE data_source_mqtt_inbound
    ADD COLUMN is_acknowledgement_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN acknowledgement_topic       TEXT    NOT NULL DEFAULT '';