CREATE TABLE si_moj_elektro.permission_event
(
    id                            bigserial PRIMARY KEY,
    dtype                         varchar(31) NOT NULL,
    permission_id                 varchar(36) NOT NULL,
    connection_id                 varchar(200),
    data_need_id                  varchar(200),
    status                        varchar(75),
    granularity                   varchar(20),
    permission_start              timestamp(6) WITH TIME ZONE,
    permission_end                timestamp(6) WITH TIME ZONE,
    event_created                 timestamp(6) WITH TIME ZONE,
    latest_meter_reading_end_date timestamp(6) WITH TIME ZONE,
    api_token                     text,
    metering_point                varchar(100),
    errors                        text
);

CREATE FUNCTION si_moj_elektro.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE si_moj_elektro.firstval_agg(anyelement)
    (SFUNC = si_moj_elektro.coalesce2, STYPE =anyelement);

CREATE VIEW si_moj_elektro.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
        si_moj_elektro.firstval_agg(connection_id)                 OVER w AS connection_id,
        si_moj_elektro.firstval_agg(data_need_id)                  OVER w AS data_need_id,
        si_moj_elektro.firstval_agg(status)                        OVER w AS status,
        si_moj_elektro.firstval_agg(granularity)                   OVER w AS granularity,
        si_moj_elektro.firstval_agg(permission_start)              OVER w AS permission_start,
        si_moj_elektro.firstval_agg(permission_end)                OVER w AS permission_end,
        si_moj_elektro.firstval_agg(latest_meter_reading_end_date) OVER w AS latest_meter_reading_end_date,
        si_moj_elektro.firstval_agg(api_token)                     OVER w AS api_token,
        si_moj_elektro.firstval_agg(metering_point)                OVER w AS metering_point,
        MIN(event_created)                                         OVER w AS created
FROM si_moj_elektro.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
