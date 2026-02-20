--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE data_source
    ADD COLUMN meter_id    TEXT NULL,
    ADD COLUMN operator_id TEXT NULL;

ALTER TABLE aiida_record
    DROP COLUMN asset,
    DROP COLUMN user_id;

ALTER TABLE inbound_record
    DROP COLUMN asset,
    DROP COLUMN user_id;