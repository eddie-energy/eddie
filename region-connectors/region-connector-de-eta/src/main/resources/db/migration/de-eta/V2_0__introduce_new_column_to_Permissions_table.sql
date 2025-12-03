ALTER TABLE de_eta.permission_event
  ADD latest_reading TIMESTAMP(6) WITH TIME ZONE;

CREATE OR REPLACE VIEW de_eta.permission_request AS
SELECT DISTINCT ON (permission_id)
       permission_id,
       de_eta.firstval_agg(connection_id) OVER w AS connection_id,
       de_eta.firstval_agg(data_need_id) OVER w  AS data_need_id,
       de_eta.firstval_agg(status) OVER w        AS status,
       de_eta.firstval_agg(data_start) OVER w    AS data_start,
       de_eta.firstval_agg(data_end) OVER w      AS data_end,
       de_eta.firstval_agg(granularity) OVER w   AS granularity,
       MIN(event_created) OVER w                   AS created,
       de_eta.firstval_agg(latest_reading) OVER w AS latest_reading
FROM de_eta.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
