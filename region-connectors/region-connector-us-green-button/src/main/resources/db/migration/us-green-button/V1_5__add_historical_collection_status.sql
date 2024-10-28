ALTER TABLE us_green_button.last_meter_readings
    ADD COLUMN historical_collection_status text;

UPDATE us_green_button.last_meter_readings mr
SET historical_collection_status = pr.polling_status
FROM us_green_button.permission_request pr
WHERE pr.permission_id = mr.permission_id;

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
                                   us_green_button.firstval_agg(auth_uid) OVER w         AS auth_uid,
                                   MIN(event_created) OVER w                             AS created
FROM us_green_button.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
