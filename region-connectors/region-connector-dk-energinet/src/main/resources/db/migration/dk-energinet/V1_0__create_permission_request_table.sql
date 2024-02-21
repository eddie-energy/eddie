CREATE TABLE energinet_customer_permission_request
(
    permission_id   varchar(36)                 NOT NULL PRIMARY KEY,
    connection_id   varchar(200)                NOT NULL,
    data_need_id    varchar(200)                NOT NULL,
    end_timestamp   timestamp(6) WITH TIME ZONE NOT NULL,
    granularity     varchar(20)                 NOT NULL,
    last_polled     timestamp(6) WITH TIME ZONE,
    metering_point  varchar(100)                NOT NULL,
    refresh_token   text                        NOT NULL,
    start_timestamp timestamp(6) WITH TIME ZONE NOT NULL,
    status          varchar(75)                 NOT NULL
);