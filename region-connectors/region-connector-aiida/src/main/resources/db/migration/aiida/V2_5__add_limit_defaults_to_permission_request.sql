--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission_event
    ADD COLUMN min_limit_kw DECIMAL DEFAULT NULL,
    ADD COLUMN max_limit_kw DECIMAL DEFAULT NULL;

DROP VIEW IF EXISTS aiida_permission_request_view;
CREATE VIEW aiida_permission_request_view AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   firstval_agg(status) OVER w           AS status,
                                   firstval_agg(connection_id) OVER w    AS connection_id,
                                   firstval_agg(data_need_id) OVER w     AS data_need_id,
                                   firstval_agg(meter_id) OVER w         AS meter_id,
                                   firstval_agg(min_limit_kw) OVER w     AS min_limit_kw,
                                   firstval_agg(max_limit_kw) OVER w     AS max_limit_kw,
                                   firstval_agg(permission_start) OVER w AS permission_start,
                                   firstval_agg(permission_end) OVER w   AS permission_end,
                                   MIN(event_created) OVER w             AS created,
                                   firstval_agg(message) OVER w          AS message,
                                   firstval_agg(aiida_id) OVER w         AS aiida_id

FROM permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
