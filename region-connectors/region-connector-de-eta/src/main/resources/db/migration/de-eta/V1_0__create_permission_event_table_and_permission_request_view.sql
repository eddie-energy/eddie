-- Create schema for German (DE) ETA Plus region connector
CREATE SCHEMA IF NOT EXISTS de_eta;

-- Create function to get first value in aggregation window
CREATE OR REPLACE FUNCTION de_eta.firstval_agg(anyelement, anyelement)
    RETURNS anyelement AS
$$
SELECT CASE WHEN $1 IS NULL THEN $2 ELSE $1 END;
$$ LANGUAGE SQL IMMUTABLE;

-- Create aggregate function
DROP AGGREGATE IF EXISTS de_eta.firstval_agg(anyelement);
CREATE AGGREGATE de_eta.firstval_agg(anyelement) (
    SFUNC = de_eta.firstval_agg,
    STYPE = anyelement
    );

-- Create permission event table for event sourcing
-- This table stores all events related to permission requests
CREATE TABLE de_eta.permission_event
(
    id                       BIGSERIAL PRIMARY KEY,
    permission_id            VARCHAR(36) NOT NULL,
    data_source_connection_id TEXT,
    connection_id            TEXT,
    metering_point_id        VARCHAR(255),
    permission_start         TIMESTAMP,
    permission_end           TIMESTAMP,
    data_start               TIMESTAMP,
    data_end                 TIMESTAMP,
    granularity              VARCHAR(50),
    energy_type              VARCHAR(50),
    status                   VARCHAR(100) NOT NULL,
    data_need_id             UUID,
    data_need_id_str         VARCHAR(36),
    event_created            TIMESTAMP   NOT NULL DEFAULT NOW(),
    message                  TEXT,
    cause                    TEXT,
    errors                   TEXT,
    dtype                    VARCHAR(31) NOT NULL DEFAULT 'DeSimpleEvent'
);

-- Create indexes for performance
CREATE INDEX idx_permission_event_permission_id ON de_eta.permission_event (permission_id);
CREATE INDEX idx_permission_event_status ON de_eta.permission_event (status);
CREATE INDEX idx_permission_event_created ON de_eta.permission_event (event_created);
CREATE INDEX idx_permission_event_metering_point ON de_eta.permission_event (metering_point_id);

-- Add composite index for better query performance on permission lookups with status
CREATE INDEX idx_permission_event_permission_id_status ON de_eta.permission_event (permission_id, status);

-- Add index for finding stale permission requests (requests that haven't been updated)
CREATE INDEX idx_permission_event_stale ON de_eta.permission_event (status, event_created) 
WHERE status IN ('REQUESTED', 'PENDING_CONSENT');

-- Add index for data source connection lookups
CREATE INDEX idx_permission_event_connection_id ON de_eta.permission_event (data_source_connection_id);

-- Create view that aggregates the latest state of each permission request
-- This view uses the event sourcing pattern to reconstruct the current state
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
                                   MIN(event_created) OVER w                              AS created,
                                   de_eta.firstval_agg(message) OVER w                    AS message,
                                   de_eta.firstval_agg(cause) OVER w                      AS cause,
                                   de_eta.firstval_agg(errors) OVER w                     AS errors
FROM de_eta.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;

-- Add comment to explain the schema
COMMENT ON SCHEMA de_eta IS 'Schema for German (DE) ETA Plus region connector';
COMMENT ON TABLE de_eta.permission_event IS 'Event sourcing table for permission requests in Germany';
COMMENT ON VIEW de_eta.eta_permission_request IS 'Aggregated view of current permission request states';
