DROP TABLE IF EXISTS es_datadis.datadis_permission_request;

CREATE TABLE es_datadis.permission_event
(
    id                   bigserial PRIMARY KEY NOT NULL,
    dtype                varchar(37)           NOT NULL,
    event_created        timestamp(6) WITH TIME ZONE,
    permission_id        varchar(36),
    connection_id        text,
    data_need_id         varchar(36),
    status               text,
    permission_start     date,
    permission_end       date,
    metering_point_id    text,
    nif                  text,
    errors               text,
    granularity          text,
    message              text,
    response             text,
    distributor_code     text,
    supply_point_type    integer,
    production_support   boolean DEFAULT FALSE,
    latest_meter_reading date
);

CREATE FUNCTION es_datadis.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE es_datadis.firstval_agg(anyelement)
    (SFUNC = es_datadis.coalesce2, STYPE =anyelement);

CREATE OR REPLACE VIEW es_datadis.datadis_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   es_datadis.firstval_agg(connection_id) OVER w        AS connection_id,
                                   es_datadis.firstval_agg(nif) OVER w                  AS nif,
                                   es_datadis.firstval_agg(metering_point_id) OVER w    AS metering_point_id,
                                   es_datadis.firstval_agg(permission_start) OVER w     AS permission_start,
                                   es_datadis.firstval_agg(permission_end) OVER w       AS permission_end,
                                   es_datadis.firstval_agg(data_need_id) OVER w         AS data_need_id,
                                   es_datadis.firstval_agg(granularity) OVER w          AS granularity,
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
