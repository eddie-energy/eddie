CREATE TABLE datadis_permission_request
(
    permission_id             varchar(36)                 NOT NULL PRIMARY KEY,
    connection_id             varchar(200)                NOT NULL,
    data_need_id              varchar(200)                NOT NULL,
    distributor_code          varchar(255)                NULL DEFAULT NULL,
    last_pulled_meter_reading timestamp(6) WITH TIME ZONE NULL DEFAULT NULL,
    measurement_type          varchar(50)                 NOT NULL,
    metering_point_id         varchar(100)                NOT NULL,
    nif                       varchar(50)                 NOT NULL,
    permission_end            timestamp(6) WITH TIME ZONE NOT NULL,
    permission_start          timestamp(6) WITH TIME ZONE NOT NULL,
    point_type                integer                     NULL DEFAULT NULL,
    request_data_from         timestamp(6) WITH TIME ZONE NOT NULL,
    request_data_to           timestamp(6) WITH TIME ZONE NOT NULL,
    status        varchar(75) NOT NULL,
    error_message TEXT        NULL DEFAULT NULL
);