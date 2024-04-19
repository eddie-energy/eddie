CREATE TABLE permission_event
(
    id                bigserial                   NOT NULL PRIMARY KEY,
    permission_id     varchar(36)                 NOT NULL,
    event_created     timestamp(6) WITH TIME ZONE NOT NULL,
    status            text,
    connection_id     text,
    data_need_id      varchar(36),
    permission_start  date,
    permission_end    date,
    termination_topic text,
    dtype             varchar(50)                 NOT NULL
);

CREATE FUNCTION coalesce2(ANYELEMENT, ANYELEMENT) RETURNS ANYELEMENT
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE firstval_agg(ANYELEMENT)
    (SFUNC = coalesce2, STYPE =ANYELEMENT);

CREATE VIEW aiida_permission_request_view AS
    SELECT DISTINCT ON (permission_id) permission_id,
                                       firstval_agg(status) OVER w            AS status,
                                       firstval_agg(connection_id) OVER w     AS connection_id,
                                       firstval_agg(data_need_id) OVER w      AS data_need_id,
                                       firstval_agg(permission_start) OVER w  AS permission_start,
                                       firstval_agg(permission_end) OVER w    AS permission_end,
                                       firstval_agg(termination_topic) OVER w AS termination_topic,
                                       firstval_agg(event_created) OVER w     AS created
    FROM permission_event
    WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
    ORDER BY permission_id, event_created;
