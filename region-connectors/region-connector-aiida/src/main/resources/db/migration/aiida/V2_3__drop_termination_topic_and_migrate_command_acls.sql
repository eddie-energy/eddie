--  SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

DROP VIEW aiida_permission_request_view;

ALTER TABLE permission_event
    DROP COLUMN termination_topic;

UPDATE aiida_mqtt_acl
SET topic = REGEXP_REPLACE(topic, '/termination$', '/command')
WHERE topic LIKE 'aiida/v1/%/termination';

CREATE VIEW aiida_permission_request_view AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   firstval_agg(status) OVER w           AS status,
                                   firstval_agg(connection_id) OVER w    AS connection_id,
                                   firstval_agg(data_need_id) OVER w     AS data_need_id,
                                   firstval_agg(permission_start) OVER w AS permission_start,
                                   firstval_agg(permission_end) OVER w   AS permission_end,
                                   MIN(event_created) OVER w             AS created,
                                   firstval_agg(message) OVER w          AS message,
                                   firstval_agg(aiida_id) OVER w         AS aiida_id
FROM permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;