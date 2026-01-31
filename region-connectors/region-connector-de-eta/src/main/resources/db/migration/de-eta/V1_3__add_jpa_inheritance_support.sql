-- Add dtype column for JPA single-table inheritance discriminator
ALTER TABLE de_eta.permission_event ADD COLUMN IF NOT EXISTS dtype VARCHAR(31);

-- Add connection_id column to match JPA field naming 
ALTER TABLE de_eta.permission_event ADD COLUMN IF NOT EXISTS connection_id TEXT;

-- Add data_need_id_str column for JPA string mapping (the existing data_need_id is UUID type)
ALTER TABLE de_eta.permission_event ADD COLUMN IF NOT EXISTS data_need_id_str VARCHAR(36);

-- Add errors column for malformed events
ALTER TABLE de_eta.permission_event ADD COLUMN IF NOT EXISTS errors TEXT;

-- Make columns nullable for JPA single-table inheritance
-- Different event types will have different fields populated
ALTER TABLE de_eta.permission_event ALTER COLUMN data_source_connection_id DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN metering_point_id DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN permission_start DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN permission_end DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN data_start DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN data_end DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN granularity DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN energy_type DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN data_need_id DROP NOT NULL;
ALTER TABLE de_eta.permission_event ALTER COLUMN created DROP NOT NULL;

-- Update existing rows to have dtype based on status
UPDATE de_eta.permission_event SET dtype = 'DeSimpleEvent' WHERE dtype IS NULL;

-- Make dtype NOT NULL after setting default values
ALTER TABLE de_eta.permission_event ALTER COLUMN dtype SET NOT NULL;

-- Update the view to include new columns
DROP VIEW IF EXISTS de_eta.eta_permission_request;

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
