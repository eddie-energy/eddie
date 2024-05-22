DROP VIEW dk_energinet.energinet_permission_request;

CREATE VIEW dk_energinet.energinet_permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   dk_energinet.firstval_agg(connection_id) OVER w                 AS connection_id,
                                   dk_energinet.firstval_agg(data_need_id) OVER w                  AS data_need_id,
                                   dk_energinet.firstval_agg(status) OVER w                        AS status,
                                   dk_energinet.firstval_agg(metering_point_id) OVER w             AS metering_point,
                                   dk_energinet.firstval_agg(permission_start) OVER w              AS permission_start,
                                   dk_energinet.firstval_agg(permission_end) OVER w                AS permission_end,
                                   dk_energinet.firstval_agg(granularity) OVER w                   AS granularity,
                                   dk_energinet.firstval_agg(refresh_token) OVER w                 AS refresh_token,
                                   dk_energinet.firstval_agg(access_token) OVER w                  AS access_token,
                                   dk_energinet.firstval_agg(latest_meter_reading_end_date) OVER w AS latest_meter_reading_end_date,
                                   dk_energinet.firstval_agg(errors) OVER w                        AS errors,
                                   MIN(event_created) OVER w                                       AS created
FROM dk_energinet.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
