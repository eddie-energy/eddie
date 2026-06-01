-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
-- SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission
    ADD COLUMN inbound_message_format TEXT;

UPDATE permission p
SET inbound_message_format = 'CIM_1_12'
FROM aiida_local_data_need dn
WHERE p.data_need_id = dn.data_need_id
  AND dn.type = 'inbound-aiida';
