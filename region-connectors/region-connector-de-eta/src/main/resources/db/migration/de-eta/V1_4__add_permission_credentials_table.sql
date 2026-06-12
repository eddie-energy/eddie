-- Credentials must not be stored in the append-only event table.
-- This migration introduces a separate mutable table for OAuth credentials,
-- migrates existing credentials for ACCEPTED permissions, and removes
-- the credential columns from the view.

CREATE TABLE de_eta.permission_credentials
(
    permission_id VARCHAR(36) PRIMARY KEY,
    access_token  TEXT        NOT NULL,
    refresh_token TEXT
);

-- The dedicated DeAcceptedEvent entity has been folded into the generic
-- DeSimpleEvent (status = ACCEPTED); re-point existing rows so the single-table
-- inheritance mapping can still resolve them.
UPDATE de_eta.permission_event
SET dtype = 'DeSimpleEvent'
WHERE dtype = 'DeAcceptedEvent';

INSERT INTO de_eta.permission_credentials (permission_id, access_token, refresh_token)
SELECT permission_id, access_token, refresh_token
FROM   de_eta.eta_permission_request
WHERE  access_token IS NOT NULL
  AND  status = 'ACCEPTED';

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