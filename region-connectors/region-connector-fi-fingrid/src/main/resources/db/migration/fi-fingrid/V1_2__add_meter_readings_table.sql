DROP VIEW fi_fingrid.permission_request;

CREATE TABLE fi_fingrid.last_meter_readings
(
    last_meter_readings     timestamp(6) WITH TIME ZONE,
    permission_id           varchar(36) NOT NULL,
    last_meter_readings_key text        NOT NULL,
    PRIMARY KEY (last_meter_readings_key, permission_id)
);

ALTER TABLE fi_fingrid.permission_event
    DROP COLUMN metering_point,
    DROP COLUMN latest_meter_reading;

CREATE OR REPLACE VIEW fi_fingrid.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   fi_fingrid.firstval_agg(connection_id) OVER w           AS connection_id,
                                   MIN(event_created) OVER w                               AS created,
                                   fi_fingrid.firstval_agg(data_need_id) OVER w            AS data_need_id,
                                   fi_fingrid.firstval_agg(granularity) OVER w             AS granularity,
                                   fi_fingrid.firstval_agg(permission_start) OVER w        AS permission_start,
                                   fi_fingrid.firstval_agg(permission_end) OVER w          AS permission_end,
                                   fi_fingrid.firstval_agg(status) OVER w                  AS status,
                                   fi_fingrid.firstval_agg(customer_identification) OVER w AS customer_identification
FROM fi_fingrid.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
