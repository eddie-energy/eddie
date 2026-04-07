--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE inbound_aiida_data_need
    RENAME COLUMN is_acknowledgement_required TO acknowledgement_required;

ALTER TABLE outbound_aiida_data_need
    RENAME COLUMN is_acknowledgement_required TO acknowledgement_required;