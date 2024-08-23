CREATE SCHEMA IF NOT EXISTS data_needs;

CREATE TABLE IF NOT EXISTS data_needs.flyway_schema_history
(
    installed_rank integer                 NOT NULL
        CONSTRAINT flyway_schema_history_pk
            PRIMARY KEY,
    version        varchar(50),
    description    varchar(200)            NOT NULL,
    type           varchar(20)             NOT NULL,
    script         varchar(1000)           NOT NULL,
    checksum       integer,
    installed_by   varchar(100)            NOT NULL,
    installed_on   timestamp DEFAULT NOW() NOT NULL,
    execution_time integer                 NOT NULL,
    success        boolean                 NOT NULL
);


CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx
    ON data_needs.flyway_schema_history (success);

CREATE TABLE IF NOT EXISTS data_needs.absolute_duration
(
    data_need_id   varchar(255) NOT NULL
        PRIMARY KEY,
    absolute_end   date         NOT NULL,
    absolute_start date         NOT NULL
);


CREATE TABLE IF NOT EXISTS data_needs.accounting_point_data_need
(
    data_need_id varchar(255) NOT NULL PRIMARY KEY,
    created_at   timestamp(6) WITH TIME ZONE,
    description  varchar(255) NOT NULL,
    name         varchar(255) NOT NULL,
    policy_link  varchar(255) NOT NULL,
    purpose      varchar(255) NOT NULL,
    enabled      boolean DEFAULT TRUE
);


CREATE TABLE IF NOT EXISTS data_needs.generic_aiida_data_need
(
    data_need_id          varchar(255) NOT NULL
        PRIMARY KEY,
    created_at            timestamp(6) WITH TIME ZONE,
    description           varchar(255) NOT NULL,
    name                  varchar(255) NOT NULL,
    policy_link           varchar(255) NOT NULL,
    purpose               varchar(255) NOT NULL,
    transmission_interval integer      NOT NULL
        CONSTRAINT generic_aiida_data_need_transmission_interval_check
            CHECK (transmission_interval >= 1),
    enabled               boolean DEFAULT TRUE
);


CREATE TABLE IF NOT EXISTS data_needs.generic_aiida_data_need_data_tags
(
    data_need_id varchar(255) NOT NULL
        CONSTRAINT fk5acf4ekqf0qurcdrx8ayq4xpk
            REFERENCES data_needs.generic_aiida_data_need,
    data_tags    varchar(255)
);


CREATE TABLE IF NOT EXISTS data_needs.relative_duration
(
    data_need_id   varchar(255) NOT NULL
        PRIMARY KEY,
    calendar_unit  varchar(255),
    relative_end   bytea,
    relative_start bytea
);


CREATE TABLE IF NOT EXISTS data_needs.smart_meter_aiida_data_need
(
    data_need_id          varchar(255) NOT NULL
        PRIMARY KEY,
    created_at            timestamp(6) WITH TIME ZONE,
    description           varchar(255) NOT NULL,
    name                  varchar(255) NOT NULL,
    policy_link           varchar(255) NOT NULL,
    purpose               varchar(255) NOT NULL,
    transmission_interval integer      NOT NULL
        CONSTRAINT smart_meter_aiida_data_need_transmission_interval_check
            CHECK (transmission_interval >= 1),
    enabled               boolean DEFAULT TRUE
);


CREATE TABLE IF NOT EXISTS data_needs.validated_consumption_data_need
(
    data_need_id    varchar(255) NOT NULL
        PRIMARY KEY,
    created_at      timestamp(6) WITH TIME ZONE,
    description     varchar(255) NOT NULL,
    name            varchar(255) NOT NULL,
    policy_link     varchar(255) NOT NULL,
    purpose         varchar(255) NOT NULL,
    energy_type     varchar(255) NOT NULL,
    max_granularity varchar(255) NOT NULL,
    min_granularity varchar(255) NOT NULL,
    enabled         boolean DEFAULT TRUE
);

-- Create the main table for RegionConnectorFilter
CREATE TABLE IF NOT EXISTS data_needs.region_connector_filter
(
    data_need_id VARCHAR(255) PRIMARY KEY,
    type         VARCHAR(255) NOT NULL
);

-- Create the table for region_connectors which is an ElementCollection
CREATE TABLE IF NOT EXISTS data_needs.region_connector_filter_ids
(
    data_need_id VARCHAR(255),
    rc_id        VARCHAR(255),
    FOREIGN KEY (data_need_id) REFERENCES region_connector_filter (data_need_id) ON DELETE CASCADE
);
