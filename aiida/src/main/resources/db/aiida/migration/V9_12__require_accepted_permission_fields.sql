--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

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

UPDATE permission
SET data_source_id = NULL
WHERE permission_id IN (SELECT permission_id FROM incomplete_permission);

-- foreign keys are checked at the end of the statement, so the order of the deletes does not matter
WITH orphan AS (SELECT inbound.id, mqtt.mqtt_connection_id, mqtt.mqtt_acl_id, inbound.mqtt_provisioning_config_id
                FROM data_source_mqtt_inbound inbound
                         JOIN data_source_mqtt mqtt ON mqtt.id = inbound.id
                WHERE inbound.permission_id IS NULL
                   OR inbound.permission_id IN (SELECT permission_id FROM incomplete_permission)),
     provisioning AS (DELETE FROM data_source_mqtt_inbound_provisioning
         WHERE id IN (SELECT mqtt_provisioning_config_id FROM orphan)
         RETURNING mqtt_connection_id, mqtt_acl_id),
     connection AS (DELETE FROM mqtt_connection
         WHERE id IN (SELECT mqtt_connection_id FROM orphan UNION SELECT mqtt_connection_id FROM provisioning)
         RETURNING mqtt_user_id),
     mqtt_user AS (DELETE FROM data_source_mqtt_user WHERE id IN (SELECT mqtt_user_id FROM connection)),
     acl AS (DELETE FROM data_source_mqtt_acl
         WHERE id IN (SELECT mqtt_acl_id FROM orphan UNION SELECT mqtt_acl_id FROM provisioning)),
     inbound AS (DELETE FROM data_source_mqtt_inbound WHERE id IN (SELECT id FROM orphan)),
     mqtt AS (DELETE FROM data_source_mqtt WHERE id IN (SELECT id FROM orphan)),
     record AS (DELETE FROM inbound_record WHERE data_source_id IN (SELECT id FROM orphan))
DELETE
FROM data_source
WHERE id IN (SELECT id FROM orphan);

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

ALTER TABLE data_source_mqtt_inbound
    ALTER COLUMN permission_id SET NOT NULL;
