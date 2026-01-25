-- V1_5: Add latest_reading (Timestamp) and cleanup old tracking table
-- This replaces the previous conflicting V1_5/V1_6 attempts.

-- 1. Add the latest_reading column (TIMESTAMP WITH TIME ZONE for ZonedDateTime)
ALTER TABLE de_eta.permission_event
    ADD COLUMN latest_reading TIMESTAMP WITH TIME ZONE;

-- 2. Drop the old view
DROP VIEW IF EXISTS de_eta.eta_permission_request;

-- 3. Recreate the VIEW with the new column
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
                                   -- New Column
                                   de_eta.firstval_agg(latest_reading) OVER w             AS latest_reading
FROM de_eta.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;

-- 4. Cleanup: Drop the old side-table from the previous implementation attempt
DROP TABLE IF EXISTS de_eta.meter_reading_tracking;