--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Discriminator for the InboundPermission and OutboundPermission subtypes, derived from the type of the data need.
ALTER TABLE permission
    ADD COLUMN type VARCHAR(16);

UPDATE permission p
SET type = CASE dn.type
               WHEN 'inbound-aiida' THEN 'inbound'
               WHEN 'outbound-aiida' THEN 'outbound'
           END
FROM aiida_local_data_need dn
WHERE p.data_need_id = dn.data_need_id;

ALTER TABLE permission
    ALTER COLUMN type SET NOT NULL,
    ADD CONSTRAINT permission_type_check CHECK (type IN ('inbound', 'outbound'));

-- Transmission control only applies to outbound permissions.
ALTER TABLE permission
    ALTER COLUMN transmission_enabled DROP NOT NULL,
    ALTER COLUMN transmission_enabled DROP DEFAULT;

UPDATE permission
SET transmission_enabled  = NULL,
    transmission_schedule = NULL
WHERE type = 'inbound';

ALTER TABLE permission
    ADD CONSTRAINT permission_transmission_enabled_check CHECK (type = 'inbound' OR transmission_enabled IS NOT NULL);
