DROP TABLE generic_aiida_data_need_data_tags;

DROP TABLE aiida_data_need_schemas;

DROP TABLE generic_aiida_data_need;

DROP TABLE smart_meter_aiida_data_need;

CREATE TABLE IF NOT EXISTS aiida_data_need
(
    data_need_id          uuid                        NOT NULL PRIMARY KEY,
    created_at            timestamp(6) WITH TIME ZONE NOT NULL,
    enabled               boolean                     NOT NULL,
    description           text                        NOT NULL,
    name                  text                        NOT NULL,
    policy_link           text                        NOT NULL,
    purpose               text                        NOT NULL,
    transmission_schedule VARCHAR(36)                 NOT NULL,
    asset                 text                        NOT NULL
);

CREATE TABLE IF NOT EXISTS aiida_data_need_data_tags
(
    data_need_id uuid NOT NULL,
    data_tag     text,
    PRIMARY KEY (data_need_id, data_tag),
    FOREIGN KEY (data_need_id) REFERENCES aiida_data_need (data_need_id)
);

CREATE TABLE IF NOT EXISTS aiida_data_need_schemas
(
    data_need_id uuid NOT NULL,
    schema       text NOT NULL,
    PRIMARY KEY (data_need_id, schema),
    FOREIGN KEY (data_need_id) REFERENCES aiida_data_need (data_need_id)
);

