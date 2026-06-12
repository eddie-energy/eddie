-- Postgres cannot drop or reorder existing columns via CREATE OR REPLACE VIEW,
-- so the view must be dropped and recreated to remove the credential columns.
DROP VIEW de_eta.eta_permission_request;
CREATE VIEW de_eta.eta_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
    de_eta.firstval_agg(connection_id)        OVER w AS data_source_connection_id,
    de_eta.firstval_agg(metering_point_id)    OVER w AS metering_point_id,
    de_eta.firstval_agg(data_start)           OVER w AS data_start,
    de_eta.firstval_agg(data_end)             OVER w AS data_end,
    de_eta.firstval_agg(granularity)          OVER w AS granularity,
    de_eta.firstval_agg(energy_type)          OVER w AS energy_type,
    de_eta.firstval_agg(status)               OVER w AS status,
    MIN(event_created)                        OVER w AS created,
    de_eta.firstval_agg(data_need_id_str)     OVER w AS data_need_id,
    de_eta.firstval_agg(message)              OVER w AS message,
    de_eta.firstval_agg(cause)                OVER w AS cause,
    de_eta.firstval_agg(latest_meter_reading) OVER w AS latest_meter_reading
FROM de_eta.permission_event
    WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;