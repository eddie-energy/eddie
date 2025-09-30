ALTER TABLE aiida_record_value
    DROP CONSTRAINT aiida_record_value_aiida_record_id_fkey,
    ADD CONSTRAINT aiida_record_value_aiida_record_id_fkey
        FOREIGN KEY (aiida_record_id)
            REFERENCES aiida_record (id)
            ON DELETE CASCADE;