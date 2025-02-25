--- Install Extension that enables the gen_random_uuid function
CREATE EXTENSION IF NOT EXISTS pgcrypto;

--- Add a new column with uuids
ALTER TABLE data_source ADD COLUMN new_id UUID;
UPDATE data_source SET new_id = gen_random_uuid();

--- Delete the old id column and set the primary key
ALTER TABLE data_source DROP COLUMN id;
ALTER TABLE data_source RENAME COLUMN new_id TO id;
ALTER TABLE data_source ADD CONSTRAINT data_source_pkey PRIMARY KEY (id);
