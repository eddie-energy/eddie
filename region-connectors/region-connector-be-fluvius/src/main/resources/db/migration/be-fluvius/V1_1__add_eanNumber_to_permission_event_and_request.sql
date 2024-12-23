DROP VIEW be_fluvius.permission_request;

CREATE TABLE be_fluvius.last_meter_readings
(
    last_meter_reading timestamp(6) WITH TIME ZONE,
    permission_id      varchar(36) NOT NULL,
    meter_ean          text        NOT NULL,
    PRIMARY KEY (meter_ean, permission_id)
);

ALTER TABLE be_fluvius.permission_event
    RENAME COLUMN permission_start TO data_start;

ALTER TABLE be_fluvius.permission_event
    RENAME COLUMN permission_end TO data_end;

CREATE VIEW be_fluvius.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   be_fluvius.firstval_agg(connection_id) OVER w        AS connection_id,
                                   be_fluvius.firstval_agg(data_need_id) OVER w         AS data_need_id,
                                   be_fluvius.firstval_agg(status) OVER w               AS status,
                                   be_fluvius.firstval_agg(data_start) OVER w AS data_start,
                                   be_fluvius.firstval_agg(data_end) OVER w   AS data_end,
                                   be_fluvius.firstval_agg(granularity) OVER w          AS granularity,
                                   be_fluvius.firstval_agg(flow) OVER w                 AS flow,
                                   be_fluvius.firstval_agg(short_url_identifier) OVER w AS short_url_identifier,
                                   MIN(event_created) OVER w                            AS created
FROM be_fluvius.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
