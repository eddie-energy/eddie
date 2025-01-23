-- REMOVE duplicates from tables until only one entry per data_need_id is left
WITH duplicates AS (SELECT ctid, -- system column that uniquely identifies a row in a table
                           data_need_id,
                           ROW_NUMBER() OVER (PARTITION BY data_need_id ORDER BY ctid) AS rn
                    FROM aiida_local_data_need_data_tags)
DELETE
FROM aiida_local_data_need_data_tags
    USING duplicates
WHERE aiida_local_data_need_data_tags.ctid = duplicates.ctid
  AND duplicates.rn > 1;

WITH duplicates AS (SELECT ctid, -- system column that uniquely identifies a row in a table
                           data_need_id,
                           ROW_NUMBER() OVER (PARTITION BY data_need_id ORDER BY ctid) AS rn
                    FROM aiida_local_data_need_schemas)
DELETE
FROM aiida_local_data_need_schemas
    USING duplicates
WHERE aiida_local_data_need_schemas.ctid = duplicates.ctid
  AND duplicates.rn > 1;

WITH duplicates AS (SELECT ctid, -- system column that uniquely identifies a row in a table
                           data_need_id,
                           ROW_NUMBER() OVER (PARTITION BY data_need_id ORDER BY ctid) AS rn
                    FROM aiida_local_data_need)
DELETE
FROM aiida_local_data_need
    USING duplicates
WHERE aiida_local_data_need.ctid = duplicates.ctid
  AND duplicates.rn > 1;

-- Remove all primary keys and foreign keys
ALTER TABLE aiida_local_data_need
    DROP CONSTRAINT aiida_local_data_need_permission_id_fkey;
ALTER TABLE aiida_local_data_need
    DROP CONSTRAINT aiida_local_data_need_pkey;
ALTER TABLE failed_to_send_entity
    DROP CONSTRAINT failed_to_send_entity_permission_id_fkey;
ALTER TABLE mqtt_streaming_config
    DROP CONSTRAINT mqtt_streaming_config_pkey;
ALTER TABLE permission
    DROP CONSTRAINT permission_pkey;

-- Drop unused columns
ALTER TABLE permission
    DROP COLUMN data_need_permission_id;
ALTER TABLE permission
    DROP COLUMN mqtt_streaming_config_permission_id;
ALTER TABLE aiida_local_data_need
    DROP COLUMN permission_id;

-- Alter column names of aiida_local_data_need subtables
ALTER TABLE aiida_local_data_need_data_tags
    RENAME COLUMN data_tags TO data_tag;
ALTER TABLE aiida_local_data_need_schemas
    RENAME COLUMN schemas TO schema;

-- Use uuid as datatype instead of varchar(36)
ALTER TABLE aiida_local_data_need
    ALTER COLUMN data_need_id TYPE uuid USING data_need_id::uuid;
ALTER TABLE aiida_local_data_need_data_tags
    ALTER COLUMN data_need_id TYPE uuid USING data_need_id::uuid;
ALTER TABLE aiida_local_data_need_schemas
    ALTER COLUMN data_need_id TYPE uuid USING data_need_id::uuid;
ALTER TABLE failed_to_send_entity
    ALTER COLUMN permission_id TYPE uuid USING permission_id::uuid;
ALTER TABLE mqtt_streaming_config
    ALTER COLUMN permission_id TYPE uuid USING permission_id::uuid;
ALTER TABLE permission
    ALTER COLUMN permission_id TYPE uuid USING permission_id::uuid;

-- Add eddie id to permission
ALTER TABLE permission
    ADD COLUMN eddie_id uuid;

-- Function to retrieve the first data need id in order to add it to the permission table
CREATE OR REPLACE FUNCTION get_first_data_need_id()
    RETURNS uuid
    LANGUAGE plpgsql
AS
$$
DECLARE
    result uuid;
BEGIN
    SELECT data_need_id
    INTO result
    FROM aiida_local_data_need
    LIMIT 1;

    RETURN result;
END;
$$;

-- Also add a data need id to the permission table
ALTER TABLE permission
    ADD COLUMN data_need_id uuid DEFAULT get_first_data_need_id();

-- Add primary keys
ALTER TABLE aiida_local_data_need
    ADD PRIMARY KEY (data_need_id);
ALTER TABLE aiida_local_data_need_data_tags
    ADD PRIMARY KEY (data_need_id, data_tag);
ALTER TABLE aiida_local_data_need_schemas
    ADD PRIMARY KEY (data_need_id, schema);
ALTER TABLE mqtt_streaming_config
    ADD PRIMARY KEY (permission_id);
ALTER TABLE permission
    ADD PRIMARY KEY (permission_id);

-- Add foreign keys
ALTER TABLE aiida_local_data_need_data_tags
    ADD FOREIGN KEY (data_need_id) REFERENCES aiida_local_data_need (data_need_id);
ALTER TABLE aiida_local_data_need_schemas
    ADD FOREIGN KEY (data_need_id) REFERENCES aiida_local_data_need (data_need_id);
ALTER TABLE failed_to_send_entity
    ADD FOREIGN KEY (permission_id) REFERENCES permission (permission_id);
ALTER TABLE mqtt_streaming_config
    ADD FOREIGN KEY (permission_id) REFERENCES permission (permission_id);
ALTER TABLE permission
    ADD FOREIGN KEY (data_need_id) REFERENCES aiida_local_data_need (data_need_id);

-- Update permission status to unfulfillable for all permissions
UPDATE permission
SET status       = 'UNFULFILLABLE',
    access_token = 'Cannot fulfill this permission because it was created with an older version of AIIDA and the permission cannot be migrated to the current AIIDA version.'
WHERE status IN ('ACCEPTED', 'WAITING_FOR_START', 'STREAMING_DATA', 'REVOCATION_RECEIVED', 'FETCHED_DETAILS');
