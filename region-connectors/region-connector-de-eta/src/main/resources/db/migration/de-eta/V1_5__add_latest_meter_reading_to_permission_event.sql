-- Add latest_meter_reading column to the permission_event table
-- This column is used to track the latest meter reading date for fulfillment checks

ALTER TABLE de_eta.permission_event
    ADD COLUMN IF NOT EXISTS latest_meter_reading DATE;

-- Drop the existing view
DROP VIEW IF EXISTS de_eta.eta_permission_request;

-- Recreate the view with the latest_meter_reading column
CREATE VIEW de_eta.eta_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   de_eta.firstval_agg(data_source_connection_id) OVER w AS data_source_connection_id,
                                   de_eta.firstval_agg(connection_id) OVER w              AS connection_id,
                                   de_eta.firstval_agg(metering_point_id) OVER w          AS metering_point_id,
                                   de_eta.firstval_agg(permission_start) OVER w           AS permission_start,
                                   de_eta.firstval_agg(permission_end) OVER w             AS permission_end,
                                   de_eta.firstval_agg(data_start) OVER w                 AS data_start,
                                   de_eta.firstval_agg(data_end) OVER w                   AS data_end,
                                   de_eta.firstval_agg(granularity) OVER w                AS granularity,
                                   de_eta.firstval_agg(energy_type) OVER w                AS energy_type,
                                   de_eta.firstval_agg(status) OVER w                     AS status,
                                   de_eta.firstval_agg(data_need_id) OVER w               AS data_need_id,
                                   de_eta.firstval_agg(data_need_id_str) OVER w           AS data_need_id_str,
                                   de_eta.firstval_agg(created) OVER w                    AS created,
                                   de_eta.firstval_agg(message) OVER w                    AS message,
                                   de_eta.firstval_agg(cause) OVER w                      AS cause,
                                   de_eta.firstval_agg(errors) OVER w                     AS errors,
                                   de_eta.firstval_agg(latest_meter_reading) OVER w       AS latest_meter_reading
FROM de_eta.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;

COMMENT ON VIEW de_eta.eta_permission_request IS 'Aggregated view of current permission request states including latest meter reading';
COMMENT ON COLUMN de_eta.permission_event.latest_meter_reading IS 'The end date of the latest meter reading received for this permission request';

