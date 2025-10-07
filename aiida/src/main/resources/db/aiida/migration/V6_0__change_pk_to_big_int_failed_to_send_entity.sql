ALTER TABLE failed_to_send_entity
    ALTER COLUMN id TYPE BIGINT;
ALTER TABLE failed_to_send_entity
    ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER SEQUENCE failed_to_send_entity_seq AS BIGINT;