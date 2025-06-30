ALTER TABLE aiida_data_need
    RENAME TO outbound_aiida_data_need;

ALTER TABLE aiida_data_need_data_tags
    DROP CONSTRAINT IF EXISTS aiida_data_need_data_tags_data_need_id_fkey;

ALTER TABLE aiida_data_need_schemas
    DROP CONSTRAINT IF EXISTS aiida_data_need_schemas_data_need_id_fkey;

CREATE TABLE IF NOT EXISTS inbound_aiida_data_need
(
    data_need_id          varchar(36)                 NOT NULL PRIMARY KEY,
    created_at            timestamp(6) WITH TIME ZONE NOT NULL,
    enabled               boolean                     NOT NULL,
    description           text                        NOT NULL,
    name                  text                        NOT NULL,
    policy_link           text                        NOT NULL,
    purpose               text                        NOT NULL,
    transmission_schedule VARCHAR(36)                 NOT NULL,
    asset                 text                        NOT NULL
);