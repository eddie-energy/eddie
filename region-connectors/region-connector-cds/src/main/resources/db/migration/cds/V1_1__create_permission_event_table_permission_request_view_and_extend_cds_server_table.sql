DROP TABLE IF EXISTS cds.coverage;
DROP TABLE IF EXISTS cds.cds_server;

CREATE TABLE cds.cds_server
(
    id                                    SERIAL PRIMARY KEY,
    base_uri                              text         NOT NULL UNIQUE,
    name                                  text         NOT NULL,
    admin_client_id                       varchar(255) NOT NULL,
    admin_client_secret                   varchar(255) NOT NULL,
    pushed_authorization_request_endpoint varchar(255) NOT NULL,
    authorization_endpoint                varchar(255) NOT NULL,
    token_endpoint                        varchar(255) NOT NULL,
    clients_endpoint                      varchar(255) NOT NULL,
    credentials_endpoint                  varchar(255) NOT NULL,
    customer_data_client_id               VARCHAR(255) NOT NULL,
    customer_data_client_secret           VARCHAR(255) NOT NULL
);

CREATE TABLE cds.coverage
(
    cds_server_id INT  NOT NULL,
    energy_type   text NOT NULL,
    PRIMARY KEY (cds_server_id, energy_type),
    FOREIGN KEY (cds_server_id) REFERENCES cds_server (id)
);

CREATE TABLE cds.oauth_credentials
(
    permission_id varchar(36) PRIMARY KEY,
    refresh_token varchar(255) NOT NULL,
    access_token  text,
    expires_at    timestamp with time zone
);

CREATE TABLE cds.permission_event
(
    id                 bigserial PRIMARY KEY,
    dtype              varchar(31) NOT NULL,
    event_created      timestamp(6) WITH TIME ZONE,
    permission_id      varchar(36),
    status             text,
    connection_id      text,
    data_need_id       varchar(36),
    data_start         date,
    data_end           date,
    granularity        text,
    cds_server_id      bigint,
    errors             text,
    auth_expires_at    timestamp(6) WITH TIME ZONE,
    state              varchar(255),
    oauth_request_type varchar(255)
);

CREATE FUNCTION cds.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE cds.firstval_agg(anyelement)
    (SFUNC = cds.coalesce2, STYPE =anyelement);

CREATE VIEW cds.permission_request AS
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
                                   MIN(event_created) OVER w                AS created
FROM cds.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
