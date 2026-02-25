--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE inbound_aiida_data_need
    ADD COLUMN is_acknowledgement_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE outbound_aiida_data_need
    ADD COLUMN is_acknowledgement_required BOOLEAN NOT NULL DEFAULT FALSE;