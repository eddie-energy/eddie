CREATE TABLE IF NOT EXISTS aiida_data_need_schemas
(
    data_need_id varchar(255) NOT NULL
    REFERENCES generic_aiida_data_need,
    schemas      varchar(255)
    );
