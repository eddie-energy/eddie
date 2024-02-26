CREATE TABLE enedis_permission_request
(
    permission_id   varchar(36)                 NOT NULL PRIMARY KEY,
    connection_id   varchar(200)                NOT NULL,
    start_timestamp timestamp(6) WITH TIME ZONE NOT NULL,
    end_timestamp   timestamp(6) WITH TIME ZONE NOT NULL,
    data_need_id    varchar(200)                NOT NULL,
    status          varchar(75)                 NOT NULL,
    granularity     varchar(75)                 NOT NULL,
    usage_point_id  varchar(36)
);