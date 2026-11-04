--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Permissions are only persisted once accepted. Permission states which no longer exist are migrated:
-- - FETCHED_MQTT_CREDENTIALS was an intermediate state of an accepted permission with MQTT credentials.
-- - Accepted permissions without MQTT credentials cannot be started.
-- - Outbound permissions could be accepted without a data source, which streams nothing.
-- - Permissions that were never accepted are revoked to keep their history.
WITH target_status AS (SELECT 'ACCEPTED' AS accepted, 'REVOKED' AS revoked),
     legacy_permission AS (SELECT p.permission_id,
                                  p.status IN (t.accepted, 'FETCHED_MQTT_CREDENTIALS')
                                      AND p.permission_id IN (SELECT m.permission_id FROM mqtt_streaming_config m)
                                      AND NOT COALESCE(p.data_source_id IS NULL AND p.data_need_id IN
                                                                                     (SELECT d.data_need_id
                                                                                      FROM aiida_local_data_need d
                                                                                      WHERE d.type = 'outbound-aiida'),
                                                       FALSE) AS startable
                           FROM permission p
                                    CROSS JOIN target_status t
                           WHERE p.status IN (t.accepted, 'FETCHED_MQTT_CREDENTIALS', 'CREATED', 'FETCHED_DETAILS',
                                              'REJECTED', 'UNFULFILLABLE'))
UPDATE permission p
SET status      = CASE WHEN l.startable THEN t.accepted ELSE t.revoked END,
    revoke_time = CASE WHEN l.startable THEN p.revoke_time ELSE COALESCE(p.revoke_time, NOW()) END
FROM legacy_permission l
         CROSS JOIN target_status t
WHERE p.permission_id = l.permission_id;

ALTER TABLE permission
    ADD CONSTRAINT permission_status_check CHECK (status IN ('ACCEPTED', 'REVOKED', 'TERMINATED', 'FULFILLED'));
