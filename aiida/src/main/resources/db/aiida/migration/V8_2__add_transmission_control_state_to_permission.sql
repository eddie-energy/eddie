--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission
    ADD COLUMN transmission_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN transmission_schedule VARCHAR;
