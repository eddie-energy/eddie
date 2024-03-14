create table at_eda.permission_event
(
    granularity   text,
    status        text,
    created           timestamp(6) with time zone,
    event_created     timestamp(6) with time zone,
    id            bigserial not null,
    permission_end    timestamp(6) with time zone,
    permission_start  timestamp(6) with time zone,
    dtype             varchar(31) not null,
    cause         text,
    cm_consent_id     varchar(255),
    cm_request_id     varchar(255),
    connection_id     varchar(255),
    conversation_id   varchar(255),
    data_need_id      varchar(255),
    dso_id        varchar(8),
    message       text,
    metering_point_id varchar(255),
    permission_id varchar(36),
    errors        text,
    primary key (id)
);

alter table if exists at_eda.malformed_event_errors
    add constraint fk_malformed_event_errors_constraint foreign key (malformed_event_id) references at_eda.permission_event;

CREATE FUNCTION at_eda.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT coalesce($1, $2)';

CREATE AGGREGATE at_eda.firstval_agg(anyelement)
    (sfunc = at_eda.coalesce2, stype =anyelement);

CREATE VIEW at_eda.eda_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   at_eda.firstval_agg(cm_request_id) OVER w     AS cm_request_id,
                                   at_eda.firstval_agg(connection_id) OVER w     AS connection_id,
                                   at_eda.firstval_agg(conversation_id) OVER w   AS conversation_id,
                                   at_eda.firstval_agg(created) OVER w           AS created,
                                   at_eda.firstval_agg(data_need_id) OVER w      AS data_need_id,
                                   at_eda.firstval_agg(dso_id) OVER w            AS dso_id,
                                   at_eda.firstval_agg(granularity) OVER w       AS granularity,
                                   at_eda.firstval_agg(metering_point_id) OVER w AS metering_point_id,
                                   at_eda.firstval_agg(permission_start) OVER w  AS permission_start,
                                   at_eda.firstval_agg(permission_end) OVER w    AS permission_end,
                                   at_eda.firstval_agg(cm_consent_id) OVER w     AS cm_consent_id,
                                   at_eda.firstval_agg(message) OVER w           AS message,
                                   at_eda.firstval_agg(status) OVER w            AS status,
                                   at_eda.firstval_agg(cause) OVER w             AS cause
FROM at_eda.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;