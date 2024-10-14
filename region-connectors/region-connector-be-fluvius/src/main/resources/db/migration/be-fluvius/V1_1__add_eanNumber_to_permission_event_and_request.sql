ALTER TABLE be_fluvius.permission_event ADD
    COLUMN ean_number text;

DROP VIEW be_fluvius.permission_request;

CREATE VIEW be_fluvius.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   be_fluvius.firstval_agg(connection_id) OVER w        AS connection_id,
                                   be_fluvius.firstval_agg(data_need_id) OVER w         AS data_need_id,
                                   be_fluvius.firstval_agg(status) OVER w               AS status,
                                   be_fluvius.firstval_agg(permission_start) OVER w     AS permission_start,
                                   be_fluvius.firstval_agg(permission_end) OVER w       AS permission_end,
                                   be_fluvius.firstval_agg(granularity) OVER w          AS granularity,
                                   be_fluvius.firstval_agg(flow) OVER w                 AS flow,
                                   be_fluvius.firstval_agg(short_url_identifier) OVER w AS short_url_identifier,
                                   be_fluvius.firstval_agg(ean_number)           OVER w AS ean_number,
                                   MIN(event_created) OVER w                            AS created
FROM be_fluvius.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
