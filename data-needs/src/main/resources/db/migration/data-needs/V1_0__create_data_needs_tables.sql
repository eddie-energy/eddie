CREATE TABLE absolute_duration
(
    data_need_id   varchar(36) NOT NULL PRIMARY KEY,
    absolute_end   date        NOT NULL,
    absolute_start date        NOT NULL
);

CREATE TABLE accounting_point_data_need
(
    data_need_id varchar(36)                 NOT NULL PRIMARY KEY,
    created_at   timestamp(6) WITH TIME ZONE NOT NULL,
    description  text                        NOT NULL,
    name         text                        NOT NULL,
    policy_link  text                        NOT NULL,
    purpose      text                        NOT NULL
);

CREATE TABLE generic_aiida_data_need
(
    data_need_id          varchar(36)                 NOT NULL PRIMARY KEY,
    created_at            timestamp(6) WITH TIME ZONE NOT NULL,
    description           text                        NOT NULL,
    name                  text                        NOT NULL,
    policy_link           text                        NOT NULL,
    purpose               text                        NOT NULL,
    transmission_interval integer                     NOT NULL
);

CREATE TABLE generic_aiida_data_need_data_tags
(
    data_need_id varchar(36) NOT NULL,
    data_tags    varchar(255),
    FOREIGN KEY (data_need_id) REFERENCES generic_aiida_data_need
);

CREATE TABLE relative_duration
(
    data_need_id   varchar(36) NOT NULL PRIMARY KEY,
    relative_end   bytea,
    relative_start bytea,
    calendar_unit  varchar(5)
);


CREATE TABLE smart_meter_aiida_data_need
(
    data_need_id          varchar(36)                 NOT NULL PRIMARY KEY,
    created_at            timestamp(6) WITH TIME ZONE NOT NULL,
    description           text                        NOT NULL,
    name                  text                        NOT NULL,
    policy_link           text                        NOT NULL,
    purpose               text                        NOT NULL,
    transmission_interval integer                     NOT NULL
);


CREATE TABLE validated_consumption_data_need
(
    data_need_id    varchar(36)                 NOT NULL PRIMARY KEY,
    created_at      timestamp(6) WITH TIME ZONE NOT NULL,
    description     text                        NOT NULL,
    name            text                        NOT NULL,
    policy_link     text                        NOT NULL,
    purpose         text                        NOT NULL,
    energy_type     varchar(50)                 NOT NULL,
    max_granularity varchar(15)                 NOT NULL,
    min_granularity varchar(15)                 NOT NULL
);
