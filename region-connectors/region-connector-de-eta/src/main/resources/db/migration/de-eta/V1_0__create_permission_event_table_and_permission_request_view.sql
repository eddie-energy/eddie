-- Create schema for German (DE) ETA Plus region connector
CREATE SCHEMA IF NOT EXISTS de_eta;

-- Create helper function for aggregating first non-null value
CREATE FUNCTION de_eta.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

-- Create aggregate function to get first value in window
CREATE AGGREGATE de_eta.firstval_agg(anyelement)
    (SFUNC = de_eta.coalesce2, STYPE = anyelement);

-- Create permission event table for event sourcing
CREATE TABLE de_eta.permission_event
(
    dtype                     VARCHAR(31)  NOT NULL,
    id                        BIGSERIAL    NOT NULL,
    event_created             TIMESTAMP(6) WITH TIME ZONE,
    permission_id             VARCHAR(36)  NOT NULL,
    connection_id             TEXT,
    data_need_id_str          VARCHAR(36),
    status                    TEXT         NOT NULL,
    metering_point_id         VARCHAR(255),
    permission_start          DATE,
    permission_end            DATE,
    granularity               TEXT,
    energy_type               TEXT,
    message                   TEXT,
    cause                     TEXT,
    errors                    TEXT,
    PRIMARY KEY (id)
);

-- Create indexes for performance
CREATE INDEX idx_permission_event_permission_id ON de_eta.permission_event (permission_id);
CREATE INDEX idx_permission_event_status ON de_eta.permission_event (status);

-- Create view that aggregates the latest state of each permission request
CREATE VIEW de_eta.eta_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   de_eta.firstval_agg(connection_id) OVER w       AS data_source_connection_id,
                                   de_eta.firstval_agg(metering_point_id) OVER w   AS metering_point_id,
                                   de_eta.firstval_agg(permission_start) OVER w    AS data_start,
                                   de_eta.firstval_agg(permission_end) OVER w      AS data_end,
                                   de_eta.firstval_agg(granularity) OVER w         AS granularity,
                                   de_eta.firstval_agg(energy_type) OVER w         AS energy_type,
                                   de_eta.firstval_agg(status) OVER w              AS status,
                                   MIN(event_created) OVER w                       AS created,
                                   de_eta.firstval_agg(data_need_id_str) OVER w    AS data_need_id,
                                   de_eta.firstval_agg(message) OVER w             AS message,
                                   de_eta.firstval_agg(cause) OVER w               AS cause
FROM de_eta.permission_event
    WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
