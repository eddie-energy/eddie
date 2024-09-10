DROP TABLE IF EXISTS us_green_button.oauth_token_details;

CREATE TABLE us_green_button.oauth_token_details
(
    permission_id           varchar(36) NOT NULL,
    access_token_expires_at timestamp(6) WITH TIME ZONE,
    access_token_issued_at  timestamp(6) WITH TIME ZONE,
    created_at              timestamp(6) WITH TIME ZONE,
    access_token_value      text,
    refresh_token_value     text,
    auth_uid                text,
    PRIMARY KEY (permission_id)
);

CREATE TABLE us_green_button.last_meter_readings
(
    last_meter_readings     timestamp(6) WITH TIME ZONE,
    permission_id           varchar(36) NOT NULL,
    last_meter_readings_key text        NOT NULL,
    PRIMARY KEY (last_meter_readings_key, permission_id)
);

ALTER TABLE us_green_button.permission_event
    ADD COLUMN polling_status text DEFAULT 'DATA_NOT_READY';

DROP VIEW us_green_button.permission_request;
CREATE OR REPLACE VIEW us_green_button.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   us_green_button.firstval_agg(connection_id) OVER w    AS connection_id,
                                   us_green_button.firstval_agg(data_need_id) OVER w     AS data_need_id,
                                   us_green_button.firstval_agg(status) OVER w           AS status,
                                   us_green_button.firstval_agg(granularity) OVER w      AS granularity,
                                   us_green_button.firstval_agg(permission_start) OVER w AS permission_start,
                                   us_green_button.firstval_agg(permission_end) OVER w   AS permission_end,
                                   us_green_button.firstval_agg(dso_id) OVER w           AS dso_id,
                                   us_green_button.firstval_agg(country_code) OVER w     AS country_code,
                                   us_green_button.firstval_agg(jump_off_url) OVER w     AS jump_off_url,
                                   us_green_button.firstval_agg(scope) OVER w            AS scope,
                                   us_green_button.firstval_agg(polling_status) OVER w   AS polling_status,
                                   MIN(event_created) OVER w                             AS created
FROM us_green_button.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
