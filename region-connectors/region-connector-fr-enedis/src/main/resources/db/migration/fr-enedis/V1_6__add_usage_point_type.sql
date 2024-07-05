/* View needs to be dropped and recreated to change the column types */
DROP VIEW IF EXISTS fr_enedis.enedis_permission_request;

ALTER TABLE fr_enedis.permission_event
    ADD COLUMN usage_point_type text DEFAULT 'CONSUMPTION';

CREATE VIEW fr_enedis.enedis_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   fr_enedis.firstval_agg(permission_start) OVER w              AS permission_start,
                                   fr_enedis.firstval_agg(permission_end) OVER w                AS permission_end,
                                   MIN(event_created) OVER w                                    AS created,
                                   fr_enedis.firstval_agg(data_need_id) OVER w                  AS data_need_id,
                                   fr_enedis.firstval_agg(granularity) OVER w                   AS granularity,
                                   fr_enedis.firstval_agg(latest_meter_reading_end_date) OVER w AS latest_meter_reading_end_date,
                                   fr_enedis.firstval_agg(connection_id) OVER w                 AS connection_id,
                                   fr_enedis.firstval_agg(status) OVER w                        AS status,
                                   fr_enedis.firstval_agg(usage_point_id) OVER w                AS usage_point_id,
                                   fr_enedis.firstval_agg(usage_point_type) OVER w              AS usage_point_type
FROM fr_enedis.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
