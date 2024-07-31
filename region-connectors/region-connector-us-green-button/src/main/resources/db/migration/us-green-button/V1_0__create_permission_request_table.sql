CREATE TABLE us_green_button.oauth_token_details
(
    permission_id           varchar(36) NOT NULL,
    access_token_expires_at timestamp(6) WITH TIME ZONE,
    access_token_issued_at  timestamp(6) WITH TIME ZONE,
    created_at              timestamp(6) WITH TIME ZONE,
    access_token_value      text,
    refresh_token_value     text,
    PRIMARY KEY (permission_id)
);

CREATE TABLE us_green_button.permission_event
(
    id               bigserial PRIMARY KEY,
    dtype            varchar(31) NOT NULL,
    event_created    timestamp(6) WITH TIME ZONE,
    permission_id    varchar(36),
    status           text,
    connection_id    text,
    data_need_id     varchar(36),
    permission_start date,
    permission_end   date,
    granularity      text,
    dso_id       text,
    country_code text,
    jump_off_url     text,
    errors           text,
    scope            text,
    invalid_reason   text
);

CREATE FUNCTION us_green_button.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE us_green_button.firstval_agg(anyelement)
    (SFUNC = us_green_button.coalesce2, STYPE =anyelement);

CREATE VIEW us_green_button.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   us_green_button.firstval_agg(connection_id) OVER w    AS connection_id,
                                   us_green_button.firstval_agg(data_need_id) OVER w     AS data_need_id,
                                   us_green_button.firstval_agg(status) OVER w           AS status,
                                   us_green_button.firstval_agg(granularity) OVER w      AS granularity,
                                   us_green_button.firstval_agg(permission_start) OVER w AS permission_start,
                                   us_green_button.firstval_agg(permission_end) OVER w   AS permission_end,
                                   us_green_button.firstval_agg(dso_id) OVER w       AS dso_id,
                                   us_green_button.firstval_agg(country_code) OVER w AS country_code,
                                   us_green_button.firstval_agg(jump_off_url) OVER w AS jump_off_url,
                                   us_green_button.firstval_agg(scope) OVER w            AS scope,
                                   MIN(event_created) OVER w                             AS created
FROM us_green_button.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;