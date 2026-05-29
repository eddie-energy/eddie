-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
-- SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission
    ADD COLUMN inbound_message_format TEXT NOT NULL DEFAULT 'CIM_1_12';
