--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE mqtt_streaming_config
    RENAME COLUMN termination_topic TO command_topic;

UPDATE mqtt_streaming_config
SET command_topic = REGEXP_REPLACE(command_topic, '/termination$', '/command/+')
WHERE command_topic LIKE 'aiida/v1/%/termination';