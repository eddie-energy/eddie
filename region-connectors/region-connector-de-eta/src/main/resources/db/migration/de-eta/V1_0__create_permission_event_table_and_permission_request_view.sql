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
    permission_id            UUID        NOT NULL,
    connection_id            UUID,
    metering_point_id        VARCHAR(255),
    permission_start         TIMESTAMP,
    permission_end           TIMESTAMP,
    data_start               TIMESTAMP,
    data_end                 TIMESTAMP,
    granularity              VARCHAR(50),
    energy_type              VARCHAR(50),
    status                   VARCHAR(100) NOT NULL,
    data_need_id             UUID,
    event_created            TIMESTAMP   NOT NULL DEFAULT NOW(),
    message                  TEXT,
    cause                    TEXT
);

-- Create indexes for performance
CREATE INDEX idx_permission_event_permission_id ON de_eta.permission_event (permission_id);
CREATE INDEX idx_permission_event_status ON de_eta.permission_event (status);
CREATE INDEX idx_permission_event_created ON de_eta.permission_event (event_created);
CREATE INDEX idx_permission_event_metering_point ON de_eta.permission_event (metering_point_id);

-- Create view that aggregates the latest state of each permission request
-- This view uses the event sourcing pattern to reconstruct the current state
CREATE VIEW de_eta.eta_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   de_eta.firstval_agg(connection_id) OVER w             AS connection_id,
                                   de_eta.firstval_agg(metering_point_id) OVER w         AS metering_point_id,
                                   de_eta.firstval_agg(permission_start) OVER w          AS permission_start,
                                   de_eta.firstval_agg(permission_end) OVER w            AS permission_end,
                                   de_eta.firstval_agg(data_start) OVER w                AS data_start,
                                   de_eta.firstval_agg(data_end) OVER w                  AS data_end,
                                   de_eta.firstval_agg(granularity) OVER w               AS granularity,
                                   de_eta.firstval_agg(energy_type) OVER w               AS energy_type,
                                   de_eta.firstval_agg(status) OVER w                    AS status,
                                   de_eta.firstval_agg(data_need_id) OVER w              AS data_need_id,
                                   MIN(event_created) OVER w                             AS created,
                                   de_eta.firstval_agg(message) OVER w                   AS message,
                                   de_eta.firstval_agg(cause) OVER w                     AS cause
FROM de_eta.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;

-- Add comment to explain the schema
COMMENT ON SCHEMA de_eta IS 'Schema for German (DE) ETA Plus region connector';
COMMENT ON TABLE de_eta.permission_event IS 'Event sourcing table for permission requests in Germany';
COMMENT ON VIEW de_eta.eta_permission_request IS 'Aggregated view of current permission request states';
