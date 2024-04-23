DROP TABLE IF EXISTS fr_enedis.enedis_permission_request;

CREATE TABLE fr_enedis.permission_event
(
    dtype
                                  VARCHAR(31)                 NOT NULL,
    id                            BIGSERIAL PRIMARY KEY       NOT NULL,
    permission_id                 VARCHAR(36)                 NOT NULL,
    permission_start              DATE,
    permission_end                DATE,
    data_need_id                  VARCHAR(36),
    granularity                   TEXT,
    latest_meter_reading_end_date DATE,
    event_created                 TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    connection_id                 TEXT,
    errors                        TEXT,
    status                        TEXT,
    usage_point_id                TEXT
);

CREATE FUNCTION fr_enedis.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE fr_enedis.firstval_agg(anyelement)
    (SFUNC = fr_enedis.coalesce2, STYPE =anyelement);


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
                                   fr_enedis.firstval_agg(usage_point_id) OVER w                AS usage_point_id
FROM fr_enedis.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
