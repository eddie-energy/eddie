-- Create schema for DE ETA if it doesn't exist
CREATE SCHEMA IF NOT EXISTS de_eta;

-- Permission event table (event-sourcing base table)
CREATE TABLE IF NOT EXISTS de_eta.permission_event
(
    id            bigserial PRIMARY KEY NOT NULL,
    dtype         varchar(31)           NOT NULL,
    event_created timestamp(6) WITH TIME ZONE,
    permission_id varchar(36),
    connection_id text,
    data_need_id  varchar(36),
    status        text,
    data_start    date,
    data_end      date,
    granularity   text
);

-- Helper function
CREATE OR REPLACE FUNCTION de_eta.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_aggregate a
                 JOIN pg_proc p ON a.aggfnoid = p.oid
        WHERE p.proname = 'firstval_agg' AND p.pronamespace = 'de_eta'::regnamespace
    ) THEN
        CREATE AGGREGATE de_eta.firstval_agg(anyelement)
            (SFUNC = de_eta.coalesce2, STYPE = anyelement);
    END IF;
END;
$$;

-- Consolidated permission_request view reflecting latest known values per permission_id
CREATE OR REPLACE VIEW de_eta.permission_request AS
SELECT DISTINCT ON (permission_id)
       permission_id,
       de_eta.firstval_agg(connection_id) OVER w AS connection_id,
       de_eta.firstval_agg(data_need_id) OVER w  AS data_need_id,
       de_eta.firstval_agg(status) OVER w        AS status,
       MIN(event_created) OVER w                 AS created,
       de_eta.firstval_agg(data_start) OVER w    AS data_start,
       de_eta.firstval_agg(data_end) OVER w      AS data_end,
       de_eta.firstval_agg(granularity) OVER w   AS granularity
FROM de_eta.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
