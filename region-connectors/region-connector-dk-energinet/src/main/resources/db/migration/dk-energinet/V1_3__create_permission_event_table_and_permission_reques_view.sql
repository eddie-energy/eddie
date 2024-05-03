DROP TABLE IF EXISTS dk_energinet.energinet_customer_permission_request;


CREATE TABLE dk_energinet.permission_event
(
    id                            bigserial PRIMARY KEY NOT NULL,
    dtype                         varchar(31)           NOT NULL,
    event_created                 timestamp(6) WITH TIME ZONE,
    permission_id                 varchar(36),
    connection_id                 text,
    data_need_id                  varchar(36),
    status                        text,
    granularity                   text,
    metering_point_id             text,
    permission_start              date,
    permission_end                date,
    refresh_token                 text,
    access_token                  text,
    latest_meter_reading_end_date date,
    errors                        text
);

CREATE FUNCTION dk_energinet.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE dk_energinet.firstval_agg(anyelement)
    (SFUNC = dk_energinet.coalesce2, STYPE =anyelement);

CREATE VIEW dk_energinet.energinet_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   dk_energinet.firstval_agg(connection_id) OVER w                 AS connection_id,
                                   dk_energinet.firstval_agg(data_need_id) OVER w                  AS data_need_id,
                                   dk_energinet.firstval_agg(status) OVER w                        AS status,
                                   dk_energinet.firstval_agg(metering_point_id) OVER w             AS metering_point,
                                   dk_energinet.firstval_agg(permission_start) OVER w              AS permission_start,
                                   dk_energinet.firstval_agg(permission_end) OVER w                AS permission_end,
                                   dk_energinet.firstval_agg(granularity) OVER w                   AS granularity,
                                   dk_energinet.firstval_agg(refresh_token) OVER w                 AS refresh_token,
                                   dk_energinet.firstval_agg(access_token) OVER w                  AS access_token,
                                   dk_energinet.firstval_agg(latest_meter_reading_end_date) OVER w AS latest_meter_reading_end_date,
                                   MIN(event_created) OVER w                                       AS created
FROM dk_energinet.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;

