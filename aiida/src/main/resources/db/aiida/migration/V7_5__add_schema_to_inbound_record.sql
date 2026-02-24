--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE inbound_record
    ADD COLUMN schema TEXT NOT NULL DEFAULT 'MIN_MAX_ENVELOPE_CIM_V1_12';