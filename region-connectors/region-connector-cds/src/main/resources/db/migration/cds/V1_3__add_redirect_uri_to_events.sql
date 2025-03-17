ALTER TABLE cds.permission_event
    ADD COLUMN redirect_uri varchar(80000);

DROP VIEW cds.permission_request;
CREATE OR REPLACE VIEW cds.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   cds.firstval_agg(connection_id) OVER w   AS connection_id,
                                   cds.firstval_agg(data_need_id) OVER w    AS data_need_id,
                                   cds.firstval_agg(status) OVER w          AS status,
                                   cds.firstval_agg(granularity) OVER w     AS granularity,
                                   cds.firstval_agg(data_start) OVER w      AS data_start,
                                   cds.firstval_agg(data_end) OVER w        AS data_end,
                                   cds.firstval_agg(cds_server_id) OVER w   AS cds_server_id,
                                   cds.firstval_agg(auth_expires_at) OVER w AS auth_expires_at,
                                   cds.firstval_agg(state) OVER w           AS state,
                                   cds.firstval_agg(redirect_uri) OVER w    AS redirect_uri,
                                   MIN(event_created) OVER w                AS created
FROM cds.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
