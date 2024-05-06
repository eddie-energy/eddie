/* View needs to be dropped and recreated to change the column types */
DROP VIEW IF EXISTS es_datadis.datadis_permission_request;

ALTER TABLE es_datadis.permission_event
    ADD COLUMN allowed_granularity text;

CREATE OR REPLACE VIEW es_datadis.datadis_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   es_datadis.firstval_agg(connection_id) OVER w        AS connection_id,
                                   es_datadis.firstval_agg(nif) OVER w                  AS nif,
                                   es_datadis.firstval_agg(metering_point_id) OVER w    AS metering_point_id,
                                   es_datadis.firstval_agg(permission_start) OVER w     AS permission_start,
                                   es_datadis.firstval_agg(permission_end) OVER w       AS permission_end,
                                   es_datadis.firstval_agg(data_need_id) OVER w         AS data_need_id,
                                   es_datadis.firstval_agg(granularity) OVER w          AS granularity,
                                   es_datadis.firstval_agg(allowed_granularity) OVER w  AS allowed_granularity,
                                   es_datadis.firstval_agg(distributor_code) OVER w     AS distributor_code,
                                   es_datadis.firstval_agg(supply_point_type) OVER w    AS point_type,
                                   es_datadis.firstval_agg(latest_meter_reading) OVER w AS latest_meter_reading,
                                   es_datadis.firstval_agg(status) OVER w               AS status,
                                   es_datadis.firstval_agg(message) OVER w              AS error_message,
                                   es_datadis.firstval_agg(production_support) OVER w   AS production_support,
                                   MIN(event_created) OVER w                            AS created
FROM es_datadis.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
