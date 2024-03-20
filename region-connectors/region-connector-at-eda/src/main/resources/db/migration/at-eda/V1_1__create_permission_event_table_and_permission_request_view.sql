/* View needs to be dropped and recreated to change the column types */
DROP VIEW IF EXISTS at_eda.eda_permission_request;

ALTER TABLE at_eda.permission_event
    ALTER COLUMN permission_start TYPE date,
    ALTER COLUMN permission_end TYPE date;


CREATE VIEW at_eda.eda_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   at_eda.firstval_agg(cm_request_id) OVER w     AS cm_request_id,
                                   at_eda.firstval_agg(connection_id) OVER w     AS connection_id,
                                   at_eda.firstval_agg(conversation_id) OVER w   AS conversation_id,
                                   at_eda.firstval_agg(created) OVER w           AS created,
                                   at_eda.firstval_agg(data_need_id) OVER w      AS data_need_id,
                                   at_eda.firstval_agg(dso_id) OVER w            AS dso_id,
                                   at_eda.firstval_agg(granularity) OVER w       AS granularity,
                                   at_eda.firstval_agg(metering_point_id) OVER w AS metering_point_id,
                                   at_eda.firstval_agg(permission_start) OVER w  AS permission_start,
                                   at_eda.firstval_agg(permission_end) OVER w    AS permission_end,
                                   at_eda.firstval_agg(cm_consent_id) OVER w     AS cm_consent_id,
                                   at_eda.firstval_agg(message) OVER w           AS message,
                                   at_eda.firstval_agg(status) OVER w            AS status,
                                   at_eda.firstval_agg(cause) OVER w             AS cause
FROM at_eda.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
