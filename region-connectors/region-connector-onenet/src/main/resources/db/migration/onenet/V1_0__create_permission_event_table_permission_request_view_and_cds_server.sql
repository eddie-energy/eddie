--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE onenet.permission_event
(
    id            bigserial PRIMARY KEY,
    dtype         varchar(31) NOT NULL,
    event_created timestamp(6) WITH TIME ZONE,
    permission_id varchar(36),
    status        text,
    connection_id text,
    data_need_id  varchar(36),
    data_start    date,
    data_end      date,
    granularity   text
);

CREATE FUNCTION onenet.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE onenet.firstval_agg(anyelement)
    (SFUNC = onenet.coalesce2, STYPE =anyelement);

CREATE VIEW onenet.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   onenet.firstval_agg(connection_id) OVER w AS connection_id,
                                   onenet.firstval_agg(data_need_id) OVER w  AS data_need_id,
                                   onenet.firstval_agg(status) OVER w        AS status,
                                   onenet.firstval_agg(data_start) OVER w    AS data_start,
                                   onenet.firstval_agg(data_end) OVER w      AS data_end,
                                   onenet.firstval_agg(granularity) OVER w   AS granularity,
                                   MIN(event_created) OVER w                 AS created
FROM onenet.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
