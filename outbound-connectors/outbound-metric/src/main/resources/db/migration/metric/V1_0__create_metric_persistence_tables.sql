CREATE TABLE metric.permission_request_metrics
(
    id                           serial           NOT NULL,
    mean                         DOUBLE PRECISION NOT NULL,
    median                       DOUBLE PRECISION NOT NULL,
    permission_request_count     INTEGER          NOT NULL DEFAULT 1,
    permission_request_status    text             NOT NULL,
    data_need_type               varchar(255)     NOT NULL,
    permission_administrator_id  varchar(255)     NOT NULL,
    region_connector_id          varchar(255)     NOT NULL,
    country_code                 varchar(2)       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (
        permission_request_status,
        data_need_type,
        permission_administrator_id,
        region_connector_id,
        country_code
    )
);

CREATE TABLE metric.permission_request_status_duration
(
    id                           bigserial    NOT NULL,
    permission_id                varchar(36)  NOT NULL,
    duration_milliseconds        BIGINT       NOT NULL,
    permission_request_status    text         NOT NULL,
    data_need_type               varchar(255) NOT NULL,
    permission_administrator_id  varchar(255) NOT NULL,
    region_connector_id          varchar(255) NOT NULL,
    country_code                 varchar(2)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (
            permission_id,
            permission_request_status
        )
);