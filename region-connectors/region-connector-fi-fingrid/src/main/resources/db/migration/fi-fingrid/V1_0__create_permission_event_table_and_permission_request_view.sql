CREATE TABLE fi_fingrid.permission_event
(
    dtype                   varchar(31) NOT NULL,
    id                      bigserial   NOT NULL,
    event_created           timestamp(6) WITH TIME ZONE,
    permission_id           varchar(36),
    status                  text,
    connection_id           varchar(255),
    customer_identification varchar(50),
    data_need_id            varchar(36),
    errors                  text,
    permission_end          date,
    granularity             text,
    permission_start        date,
    PRIMARY KEY (id)
);

CREATE FUNCTION fi_fingrid.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE fi_fingrid.firstval_agg(anyelement)
    (SFUNC = fi_fingrid.coalesce2, STYPE =anyelement);

CREATE VIEW fi_fingrid.permission_request AS
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
