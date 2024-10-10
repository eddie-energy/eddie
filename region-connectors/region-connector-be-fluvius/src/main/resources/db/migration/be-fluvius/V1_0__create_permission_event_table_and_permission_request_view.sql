CREATE TABLE be_fluvius.permission_event
(
    dtype                varchar(31) NOT NULL,
    id                   bigserial   NOT NULL,
    event_created        timestamp(6) WITH TIME ZONE,
    permission_id        varchar(36) NOT NULL,
    connection_id        text,
    data_need_id         varchar(36),
    status               text        NOT NULL,
    permission_start     date,
    permission_end       date,
    granularity          text,
    flow                 text,
    short_url_identifier text,
    invalid_message      text,
    errors               text,
    PRIMARY KEY (id)
);

CREATE FUNCTION be_fluvius.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE be_fluvius.firstval_agg(anyelement)
    (SFUNC = be_fluvius.coalesce2, STYPE =anyelement);

CREATE VIEW be_fluvius.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   be_fluvius.firstval_agg(connection_id) OVER w        AS connection_id,
                                   be_fluvius.firstval_agg(data_need_id) OVER w         AS data_need_id,
                                   be_fluvius.firstval_agg(status) OVER w               AS status,
                                   be_fluvius.firstval_agg(permission_start) OVER w     AS permission_start,
                                   be_fluvius.firstval_agg(permission_end) OVER w       AS permission_end,
                                   be_fluvius.firstval_agg(granularity) OVER w          AS granularity,
                                   be_fluvius.firstval_agg(flow) OVER w                 AS flow,
                                   be_fluvius.firstval_agg(short_url_identifier) OVER w AS short_url_identifier,
                                   MIN(event_created) OVER w                            AS created
FROM be_fluvius.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
