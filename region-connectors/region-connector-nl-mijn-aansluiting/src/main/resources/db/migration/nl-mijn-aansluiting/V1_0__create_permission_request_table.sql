CREATE TABLE nl_mijn_aansluiting.oauth_token_details
(
    access_token_expires_at timestamp(6) WITH TIME ZONE,
    access_token_issued_at  timestamp(6) WITH TIME ZONE,
    created_at              timestamp(6) WITH TIME ZONE,
    refresh_token_issued_at timestamp(6) WITH TIME ZONE,
    access_token_value      text,
    permission_id           varchar(36) NOT NULL,
    refresh_token_value     text,
    PRIMARY KEY (permission_id)
);

CREATE TABLE nl_mijn_aansluiting.permission_event
(
    granularity      text,
    permission_end   date,
    permission_start date,
    created          timestamp(6) WITH TIME ZONE,
    event_created    timestamp(6) WITH TIME ZONE,
    id               bigserial PRIMARY KEY,
    dtype            varchar(31) NOT NULL,
    code_verifier    text,
    connection_id    text,
    data_need_id     varchar(36),
    errors           text,
    permission_id    varchar(36),
    state            text,
    status           text
);

CREATE TABLE nl_mijn_aansluiting.last_meter_readings
(
    last_meter_readings     timestamp(6) WITH TIME ZONE,
    permission_id           varchar(36) NOT NULL,
    last_meter_readings_key text        NOT NULL,
    PRIMARY KEY (last_meter_readings_key, permission_id)
);

CREATE FUNCTION nl_mijn_aansluiting.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE nl_mijn_aansluiting.firstval_agg(anyelement)
    (SFUNC = nl_mijn_aansluiting.coalesce2, STYPE =anyelement);

CREATE VIEW nl_mijn_aansluiting.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   nl_mijn_aansluiting.firstval_agg(connection_id) OVER w    AS connection_id,
                                   nl_mijn_aansluiting.firstval_agg(created) OVER w          AS created,
                                   nl_mijn_aansluiting.firstval_agg(data_need_id) OVER w     AS data_need_id,
                                   nl_mijn_aansluiting.firstval_agg(granularity) OVER w      AS granularity,
                                   nl_mijn_aansluiting.firstval_agg(permission_start) OVER w AS permission_start,
                                   nl_mijn_aansluiting.firstval_agg(permission_end) OVER w   AS permission_end,
                                   nl_mijn_aansluiting.firstval_agg(status) OVER w           AS status,
                                   nl_mijn_aansluiting.firstval_agg(state) OVER w            AS state,
                                   nl_mijn_aansluiting.firstval_agg(code_verifier) OVER w    AS code_verifier
FROM nl_mijn_aansluiting.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;