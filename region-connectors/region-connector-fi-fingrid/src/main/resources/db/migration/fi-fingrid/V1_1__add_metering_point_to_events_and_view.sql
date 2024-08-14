ALTER TABLE fi_fingrid.permission_event
    ADD metering_point       VARCHAR(18),
    ADD latest_meter_reading TIMESTAMP(6) WITH TIME ZONE;


CREATE OR REPLACE VIEW fi_fingrid.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   fi_fingrid.firstval_agg(connection_id) OVER w           AS connection_id,
                                   MIN(event_created) OVER w                               AS created,
                                   fi_fingrid.firstval_agg(data_need_id) OVER w            AS data_need_id,
                                   fi_fingrid.firstval_agg(granularity) OVER w             AS granularity,
                                   fi_fingrid.firstval_agg(permission_start) OVER w        AS permission_start,
                                   fi_fingrid.firstval_agg(permission_end) OVER w          AS permission_end,
                                   fi_fingrid.firstval_agg(status) OVER w                  AS status,
                                   fi_fingrid.firstval_agg(customer_identification) OVER w AS customer_identification,
                                   fi_fingrid.firstval_agg(metering_point) OVER w          AS metering_point,
                                   fi_fingrid.firstval_agg(latest_meter_reading) OVER w    AS latest_meter_reading
FROM fi_fingrid.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
