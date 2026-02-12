-- Change permission_id from UUID to VARCHAR(36) for JPA compatibility
-- This aligns with the EDDIE framework pattern used by other region connectors

-- Drop the view first (required before altering column type)
DROP VIEW IF EXISTS de_eta.eta_permission_request;

-- Drop indexes that depend on the column
DROP INDEX IF EXISTS de_eta.idx_permission_event_permission_id;
DROP INDEX IF EXISTS de_eta.idx_permission_event_permission_id_status;

-- Change the column type from UUID to VARCHAR(36)
ALTER TABLE de_eta.permission_event 
    ALTER COLUMN permission_id TYPE VARCHAR(36) USING permission_id::text;

-- Recreate the indexes
CREATE INDEX idx_permission_event_permission_id ON de_eta.permission_event (permission_id);
CREATE INDEX idx_permission_event_permission_id_status ON de_eta.permission_event (permission_id, status);

-- Recreate the view
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
                                   de_eta.firstval_agg(errors) OVER w                     AS errors
FROM de_eta.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;

COMMENT ON VIEW de_eta.eta_permission_request IS 'Aggregated view of current permission request states';
