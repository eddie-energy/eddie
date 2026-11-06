--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Connection states are no longer persisted as permission status, but derived at runtime. FETCHED_MQTT_CREDENTIALS
-- was an intermediate state of an accepted permission with MQTT credentials.
UPDATE permission
SET status = 'ACCEPTED'
WHERE status IN ('FETCHED_MQTT_CREDENTIALS', 'WAITING_FOR_START', 'STREAMING_DATA', 'FAILED_TO_START');

-- Permissions are only persisted once accepted. Permissions that cannot be accepted or started are deleted:
-- - Permissions that were never accepted (CREATED, FETCHED_DETAILS, REJECTED, UNFULFILLABLE).
-- - Accepted permissions without MQTT credentials.
-- - Accepted outbound permissions without a data source. Inbound data sources are created on demand.
CREATE TEMPORARY TABLE deleted_permission ON COMMIT DROP AS
SELECT p.permission_id
FROM permission p
WHERE p.status NOT IN ('REVOKED', 'TERMINATED', 'FULFILLED')
  AND (p.status IN ('CREATED', 'FETCHED_DETAILS', 'REJECTED', 'UNFULFILLABLE')
    OR p.permission_id NOT IN (SELECT m.permission_id FROM mqtt_streaming_config m)
    OR (p.data_source_id IS NULL AND p.data_need_id IN (SELECT d.data_need_id
                                                        FROM aiida_local_data_need d
                                                        WHERE d.type = 'outbound-aiida')));

DELETE FROM connection_limit WHERE permission_id IN (SELECT permission_id FROM deleted_permission);
DELETE FROM connection_limit_default WHERE permission_id IN (SELECT permission_id FROM deleted_permission);
DELETE FROM failed_to_send_entity WHERE permission_id IN (SELECT permission_id FROM deleted_permission);
UPDATE data_source_mqtt_inbound
SET permission_id = NULL
WHERE permission_id IN (SELECT permission_id FROM deleted_permission);
DELETE FROM permission WHERE permission_id IN (SELECT permission_id FROM deleted_permission);

ALTER TABLE permission
    ADD CONSTRAINT permission_status_check CHECK (status IN ('ACCEPTED', 'REVOKED', 'TERMINATED', 'FULFILLED'));
