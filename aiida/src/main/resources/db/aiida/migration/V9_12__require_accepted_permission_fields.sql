--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Permissions are only persisted once accepted, so the fields known after accepting are required. Old historical
-- permissions may lack some of them: derivable values are backfilled, remaining incomplete permissions are deleted.
UPDATE permission p
SET service_name = dn.name
FROM aiida_local_data_need dn
WHERE p.data_need_id = dn.data_need_id
  AND p.service_name IS NULL;

UPDATE permission
SET display_name = service_name
WHERE display_name IS NULL;

UPDATE permission
SET grant_time = start_time
WHERE grant_time IS NULL;

CREATE TEMPORARY TABLE incomplete_permission ON COMMIT DROP AS
SELECT permission_id
FROM permission
WHERE eddie_id IS NULL
   OR data_need_id IS NULL
   OR connection_id IS NULL
   OR start_time IS NULL
   OR expiration_time IS NULL
   OR grant_time IS NULL
   OR service_name IS NULL
   OR display_name IS NULL;

DELETE FROM connection_limit WHERE permission_id IN (SELECT permission_id FROM incomplete_permission);
DELETE FROM connection_limit_default WHERE permission_id IN (SELECT permission_id FROM incomplete_permission);
DELETE FROM failed_to_send_entity WHERE permission_id IN (SELECT permission_id FROM incomplete_permission);
UPDATE data_source_mqtt_inbound
SET permission_id = NULL
WHERE permission_id IN (SELECT permission_id FROM incomplete_permission);
DELETE FROM permission WHERE permission_id IN (SELECT permission_id FROM incomplete_permission);

ALTER TABLE permission
    ALTER COLUMN eddie_id SET NOT NULL,
    ALTER COLUMN data_need_id SET NOT NULL,
    ALTER COLUMN connection_id SET NOT NULL,
    ALTER COLUMN start_time SET NOT NULL,
    ALTER COLUMN expiration_time SET NOT NULL,
    ALTER COLUMN grant_time SET NOT NULL,
    ALTER COLUMN service_name SET NOT NULL,
    ALTER COLUMN display_name SET NOT NULL;
